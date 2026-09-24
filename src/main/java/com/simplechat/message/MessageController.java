package com.simplechat.message;

import com.simplechat.security.AuthPrincipal;
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
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/{peerId}")
    public List<MessageDto> history(@AuthenticationPrincipal AuthPrincipal current, @PathVariable Long peerId) {
        return messageService.history(current.id(), peerId);
    }
}
