package com.simplechat.message;

import com.simplechat.exception.ApiException;
import com.simplechat.user.User;
import com.simplechat.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessage createSent(Long senderId, Long recipientId, String text) {
        if (senderId.equals(recipientId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "Cannot send a message to yourself");
        }
        User sender = requireUser(senderId);
        User recipient = requireUser(recipientId);
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setText(text);
        message.setStatus(MessageStatus.SENT);
        message.setCreatedAt(Instant.now());
        return chatMessageRepository.save(message);
    }

    @Transactional
    public ChatMessage markDelivered(ChatMessage message) {
        message.setStatus(MessageStatus.DELIVERED);
        return chatMessageRepository.save(message);
    }

    @Transactional
    public List<ChatMessage> deliverPending(Long recipientId) {
        List<ChatMessage> pending =
                chatMessageRepository.findPendingForRecipient(recipientId, MessageStatus.SENT);
        pending.forEach(message -> message.setStatus(MessageStatus.DELIVERED));
        return chatMessageRepository.saveAll(pending);
    }

    @Transactional
    public List<ChatMessage> markConversationRead(Long readerId, Long peerId) {
        List<ChatMessage> unread = chatMessageRepository.findIncomingWithStatuses(
                peerId, readerId, List.of(MessageStatus.SENT, MessageStatus.DELIVERED));
        unread.forEach(message -> message.setStatus(MessageStatus.READ));
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
