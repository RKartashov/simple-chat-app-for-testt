package com.simplechat.service;

import com.simplechat.common.EntityService;
import com.simplechat.domain.entity.ChatMessage;
import com.simplechat.domain.entity.ChatMessageStatus;
import com.simplechat.domain.entity.Conversation;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.ChatMessageRepository;
import com.simplechat.exception.ApiException;
import com.simplechat.rest.dto.MessageDto;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService implements EntityService<ChatMessage, Long> {

    @Getter
    private final ChatMessageRepository repository;
    private final UserService userService;
    private final ConversationService conversationService;

    public ChatMessage createMessage(Long senderId, Long recipientId, String text, boolean isDelivered) {
        if (senderId.equals(recipientId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot send a message to yourself");
        }

        User sender = userService.getById(senderId);
        User recipient = userService.getById(recipientId);
        Conversation conversation = conversationService.ensureConversation(sender, recipient);
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setText(text);
        message.setStatus(isDelivered ? ChatMessageStatus.DELIVERED : ChatMessageStatus.SENT);
        message.setCreatedAt(Instant.now());
        message.setConversation(conversation);

        return save(message);
    }

    public List<ChatMessage> markPendingAsDelivered(Long recipientId) {
        List<ChatMessage> pending = repository.findPendingForRecipient(recipientId);
        pending.forEach(message -> message.setStatus(ChatMessageStatus.DELIVERED));

        return saveAll(pending);
    }

    public List<ChatMessage> markMessagesAsRead(Long readerId, Long peerId) {
        Long conversationId = conversationService.getByUserIds(readerId, peerId).getId();
        List<ChatMessage> unread = repository.findUnreadInConversation(conversationId, readerId);
        unread.forEach(message -> message.setStatus(ChatMessageStatus.READ));

        return saveAll(unread);
    }

    /**
     * Получить все сообщения между читателем и другим пользователем
     */
    public List<MessageDto> getMessagesHistory(Long readerId, Long peerId) {
        User reader = userService.getById(readerId);
        User peer = userService.getById(peerId);
        Long conversationId = conversationService.ensureConversation(reader, peer).getId();

        return repository.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream()
                         .map(MessageDto::from)
                         .toList();
    }

}
