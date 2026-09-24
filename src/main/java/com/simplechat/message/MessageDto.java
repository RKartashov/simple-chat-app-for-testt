package com.simplechat.message;

import java.time.Instant;

public record MessageDto(
        Long id, Long fromUserId, Long toUserId, String text, MessageStatus status, Instant createdAt) {

    public static MessageDto from(ChatMessage message) {
        return new MessageDto(
                message.getId(),
                message.getSender().getId(),
                message.getRecipient().getId(),
                message.getText(),
                message.getStatus(),
                message.getCreatedAt());
    }
}
