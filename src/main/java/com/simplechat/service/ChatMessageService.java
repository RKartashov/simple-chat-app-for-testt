package com.simplechat.service;

import com.simplechat.domain.entity.ChatMessage;
import com.simplechat.domain.entity.ChatMessageStatus;
import com.simplechat.exception.ApiException;
import com.simplechat.domain.entity.User;
import com.simplechat.rest.dto.MessageDto;
import com.simplechat.domain.repository.ChatMessageRepository;
import com.simplechat.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessage createMessage(Long senderId, Long recipientId, String text, boolean isDelivered) {
        if (senderId.equals(recipientId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot send a message to yourself");
        }

        User sender = requireUser(senderId);
        User recipient = requireUser(recipientId);
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setText(text);
        message.setStatus(isDelivered ? ChatMessageStatus.DELIVERED : ChatMessageStatus.SENT);
        message.setCreatedAt(Instant.now());
        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatMessage markDelivered(ChatMessage message) {
        message.setStatus(ChatMessageStatus.DELIVERED);
        return chatMessageRepository.save(message);
    }

    @Transactional
    public List<ChatMessage> deliverPending(Long recipientId) {
        List<ChatMessage> pending = chatMessageRepository.findPendingForRecipient(recipientId, ChatMessageStatus.SENT);
        pending.forEach(message -> message.setStatus(ChatMessageStatus.DELIVERED));
        return chatMessageRepository.saveAll(pending);
    }

    @Transactional
    public List<ChatMessage> markConversationRead(Long readerId, Long peerId) {
        List<ChatMessage> unread = chatMessageRepository.findIncomingWithStatuses(
                peerId, readerId, List.of(ChatMessageStatus.SENT, ChatMessageStatus.DELIVERED));
        unread.forEach(message -> message.setStatus(ChatMessageStatus.READ));
        return chatMessageRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> history(Long currentUserId, Long peerId) {
        requireUser(peerId);
        return chatMessageRepository.findConversation(currentUserId, peerId).stream()
                .map(MessageDto::from)
                .toList();
    }

    private User requireUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "User not found"));
    }
}
