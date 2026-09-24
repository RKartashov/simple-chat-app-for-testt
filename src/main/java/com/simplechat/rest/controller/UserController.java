package com.simplechat.rest.controller;

import com.simplechat.rest.dto.UserDto;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> list(
        @AuthenticationPrincipal AuthPrincipal current,
        @RequestParam(required = false) String query
    ) {
        return userService.getOtherUsers(current, query);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthPrincipal current) {
        return userService.getCurrentUser(current);
    }

}
