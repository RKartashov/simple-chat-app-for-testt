package com.simplechat.rest.controller;

import com.simplechat.rest.dto.MessageDto;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.service.ChatMessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService messageService;

    @GetMapping("/{peerId}")
    public List<MessageDto> getMessageHistory(
        @AuthenticationPrincipal AuthPrincipal current,
        @PathVariable Long peerId
    ) {
        return messageService.getMessagesHistory(current.id(), peerId);
    }

}
