package com.simplechat.websocket;

import com.simplechat.domain.entity.ChatMessage;
import com.simplechat.rest.dto.MessageDto;
import com.simplechat.service.ChatMessageService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ChatRealtimeService {

    private final ChatMessageService messageService;
    private final WebSocketSessionRegistry sessionRegistry;
    private final JsonMapper jsonMapper;

    @Transactional
    public void sendChatMessage(Long fromUserId, Long toUserId, String text) {
        boolean deliverImmediately = sessionRegistry.isOnline(toUserId);
        ChatMessage message = messageService.createMessage(fromUserId, toUserId, text, deliverImmediately);

        if (deliverImmediately) {
            pushMessage(toUserId, message);
        }
        pushMessage(fromUserId, message);
    }

    @Transactional
    public void deliverPending(Long recipientId) {
        List<ChatMessage> delivered = messageService.deliverPending(recipientId);
        for (ChatMessage message : delivered) {
            pushMessage(recipientId, message);
            pushStatus(message.getSender().getId(), message);
        }
    }

    @Transactional
    public void markRead(Long readerId, Long peerId) {
        List<ChatMessage> readMessages = messageService.markConversationRead(readerId, peerId);
        for (ChatMessage message : readMessages) {
            pushStatus(message.getSender().getId(), message);
            pushStatus(readerId, message);
        }
    }

    public void broadcastPresence(Long userId, boolean online) {
        sendJson(null, WsFrame.presence(userId, online), true, userId);
    }

    private void pushMessage(Long userId, ChatMessage message) {
        sendJson(userId, WsFrame.message(MessageDto.from(message)), false, null);
    }

    private void pushStatus(Long userId, ChatMessage message) {
        sendJson(userId, WsFrame.status(message.getId(), message.getStatus().name()), false, null);
    }

    private void sendJson(Long userId, WsFrame frame, boolean broadcast, Long exceptUserId) {
        try {
            String payload = jsonMapper.writeValueAsString(frame);
            if (broadcast) {
                sessionRegistry.broadcast(payload, exceptUserId);
            } else {
                sessionRegistry.sendToUser(userId, payload);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize WebSocket frame", ex);
        }
    }

}
