package com.simplechat.websocket;

import com.simplechat.exception.ApiException;
import com.simplechat.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    static final String PRINCIPAL_ATTR = "authPrincipal";

    private final WebSocketSessionRegistry sessionRegistry;
    private final ChatRealtimeService chatRealtimeService;
    private final JsonMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        ConcurrentWebSocketSessionDecorator decoratedSession = new ConcurrentWebSocketSessionDecorator(
            session,
            1000,
            1024 * 1024
        );

        AuthPrincipal principal = getPrincipal(decoratedSession);
        try {
            sessionRegistry.register(principal.id(), decoratedSession);
            chatRealtimeService.deliverPending(principal.id());
            chatRealtimeService.broadcastPresence(principal.id(), true);
        } catch (Exception ex) {
            log.error("Failed to initialize WebSocket session for user {}", principal.id(), ex);
            sendError(decoratedSession, "Failed to initialize chat session");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AuthPrincipal principal = getPrincipal(session);
        try {
            WsFrame frame = jsonMapper.readValue(message.getPayload(), WsFrame.class);
            String type = frame.type() == null ? "" : frame.type().toLowerCase();
            switch (type) {
                case "chat" -> handleChat(principal, frame);
                case "read" -> handleRead(principal, frame);
                default -> sendError(session, "Unknown message type");
            }
        } catch (ApiException ex) {
            sendError(session, ex.getMessage());
        } catch (Exception ex) {
            log.warn("WebSocket message handling failed for user {}", principal.id(), ex);
            sendError(session, "Failed to process message");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error in session {}", session.getId(), exception);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception ignored) {
            // already closing
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AuthPrincipal principal = (AuthPrincipal) session.getAttributes().get(PRINCIPAL_ATTR);
        if (principal == null) {
            return;
        }
        sessionRegistry.unregister(principal.id(), session);
        if (!sessionRegistry.isUserOnline(principal.id())) {
            chatRealtimeService.broadcastPresence(principal.id(), false);
        }
    }

    private void handleChat(AuthPrincipal principal, WsFrame frame) {
        if (frame.toUserId() == null || frame.text() == null || frame.text().isBlank()) {
            throw new ApiException(400, "Chat frame requires toUserId and text");
        }
        if (frame.text().length() > 4000) {
            throw new ApiException(400, "Message is too long");
        }
        chatRealtimeService.sendChatMessage(principal.id(), frame.toUserId(), frame.text().trim());
    }

    private void handleRead(AuthPrincipal principal, WsFrame frame) {
        Long peerId = frame.peerId() != null ? frame.peerId() : frame.fromUserId();
        if (peerId == null) {
            throw new ApiException(400, "Read frame requires peerId");
        }
        chatRealtimeService.readDelivered(principal.id(), peerId);
    }

    private AuthPrincipal getPrincipal(WebSocketSession session) {
        AuthPrincipal principal = (AuthPrincipal) session.getAttributes().get(PRINCIPAL_ATTR);
        if (principal == null) {
            throw new ApiException(401, "Unauthenticated WebSocket session");
        }
        return principal;
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            sessionRegistry.sendWebSocketMessage(session, jsonMapper.writeValueAsString(WsFrame.error(message)));
        } catch (Exception ex) {
            log.warn("Failed to send WebSocket error frame", ex);
        }
    }

}
