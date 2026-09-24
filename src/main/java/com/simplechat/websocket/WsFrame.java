package com.simplechat.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simplechat.rest.dto.MessageDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WsFrame(
        String type,
        Long id,
        Long fromUserId,
        Long toUserId,
        Long peerId,
        Long userId,
        Long messageId,
        String text,
        String status,
        String message,
        Boolean online,
        String createdAt) {

    public static WsFrame chat(Long toUserId, String text) {
        return new WsFrame("chat", null, null, toUserId, null, null, null, text, null, null, null, null);
    }

    public static WsFrame message(MessageDto dto) {
        return new WsFrame(
                "message",
                dto.id(),
                dto.fromUserId(),
                dto.toUserId(),
                null,
                null,
                null,
                dto.text(),
                dto.status().name(),
                null,
                null,
                dto.createdAt().toString());
    }

    public static WsFrame status(Long messageId, String status) {
        return new WsFrame("status", null, null, null, null, null, messageId, null, status, null, null, null);
    }

    public static WsFrame presence(Long userId, boolean online) {
        return new WsFrame("presence", null, null, null, null, userId, null, null, null, null, online, null);
    }

    public static WsFrame error(String message) {
        return new WsFrame("error", null, null, null, null, null, null, null, null, message, null, null);
    }
}
