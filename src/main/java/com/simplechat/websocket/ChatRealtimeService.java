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
    public void sendChatMessage(Long senderId, Long recipientId, String text) {
        boolean deliverImmediately = sessionRegistry.isUserOnline(recipientId);
        ChatMessage message = messageService.createMessage(senderId, recipientId, text, deliverImmediately);

        if (deliverImmediately) {
            pushMessage(recipientId, message);
        }
        pushMessage(senderId, message);
    }

    /**
     * Доставить сообщения со статусом Отправлено пользователю
     */
    @Transactional
    public void deliverPending(Long recipientId) {
        List<ChatMessage> delivered = messageService.markPendingAsDelivered(recipientId);
        for (ChatMessage message : delivered) {
            pushMessage(recipientId, message);
            pushMessageStatus(message.getSender().getId(), message);
        }
    }

    /**
     * Пометить доставленные читателю сообщения, как прочитанные
     */
    @Transactional
    public void readDelivered(Long readerId, Long peerId) {
        List<ChatMessage> readMessages = messageService.markMessagesAsRead(readerId, peerId);
        for (ChatMessage message : readMessages) {
            pushMessageStatus(message.getSender().getId(), message);
            pushMessageStatus(readerId, message);
        }
    }

    /**
     * Сообщить об обновлении статуса пользователя
     */
    public void broadcastPresence(Long userId, boolean isOnline) {
        sendJson(null, WsFrame.presence(userId, isOnline), true, userId);
    }

    /**
     * Направить сообщение чата пользователю
     */
    private void pushMessage(Long userId, ChatMessage message) {
        sendJson(userId, WsFrame.message(MessageDto.from(message)), false, null);
    }

    /**
     * Сообщить об обновлении статуса сообщения
     */
    private void pushMessageStatus(Long userId, ChatMessage message) {
        sendJson(userId, WsFrame.status(message.getId(), message.getStatus().name()), false, null);
    }

    /**
     * Отправка данных по WebSocket
     */
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
