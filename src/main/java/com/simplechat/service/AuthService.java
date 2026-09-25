package com.simplechat.service;

import com.simplechat.domain.entity.User;
import com.simplechat.exception.ApiException;
import com.simplechat.rest.dto.AuthRequest;
import com.simplechat.rest.dto.AuthResponse;
import com.simplechat.rest.dto.UserDto;
import com.simplechat.security.JwtService.IssuedToken;
import com.simplechat.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final WebSocketSessionRegistry sessionRegistry;

    public AuthResponse register(AuthRequest request) {
        if (userService.existsByNickname(request.nickname())) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "Nickname is already taken");
        }

        User user = userService.createUser(request.nickname(), passwordEncoder.encode(request.password()));

        return getLoginResponse(user, authSessionService.openSession(user));
    }

    public AuthResponse login(AuthRequest request) {
        User user = userService
            .findByNickname(request.nickname())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "Invalid nickname or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "Invalid nickname or password");
        }

        return getLoginResponse(user, authSessionService.openSession(user));
    }

    private AuthResponse getLoginResponse(User user, IssuedToken issued) {
        return new AuthResponse(
            issued.token(),
            new UserDto(user.getId(), user.getNickname(), sessionRegistry.isUserOnline(user.getId()))
        );
    }

}
