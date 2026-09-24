package com.simplechat.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.simplechat.rest.dto.MessageDto;
import lombok.Builder;

@Builder
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
    Boolean isOnline,
    String createdAt
) {

    public static WsFrame message(MessageDto dto) {
        return WsFrame.builder()
                      .type("message")
                      .id(dto.id())
                      .fromUserId(dto.fromUserId())
                      .toUserId(dto.toUserId())
                      .text(dto.text())
                      .status(dto.status().name())
                      .createdAt(dto.createdAt().toString())
                      .build();
    }

    public static WsFrame status(Long messageId, String status) {
        return WsFrame.builder()
                      .type("status")
                      .messageId(messageId)
                      .status(status)
                      .build();
    }

    public static WsFrame presence(Long userId, boolean isOnline) {
        return WsFrame.builder()
                      .type("presence")
                      .userId(userId)
                      .isOnline(isOnline)
                      .build();
    }

    public static WsFrame error(String message) {
        return WsFrame.builder()
                      .type("error")
                      .message(message)
                      .build();
    }

}
