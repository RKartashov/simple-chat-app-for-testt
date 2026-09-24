package com.simplechat.rest.dto;

import com.simplechat.domain.entity.ChatMessage;
import com.simplechat.domain.entity.ChatMessageStatus;

import java.time.Instant;

public record MessageDto(
        Long id, Long fromUserId, Long toUserId, String text, ChatMessageStatus status, Instant createdAt) {

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
