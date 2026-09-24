package com.simplechat.auth;

import com.simplechat.exception.ApiException;
import com.simplechat.security.JwtService.IssuedToken;
import com.simplechat.session.AuthSessionService;
import com.simplechat.user.User;
import com.simplechat.user.UserDto;
import com.simplechat.user.UserRepository;
import com.simplechat.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final WebSocketSessionRegistry sessionRegistry;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByNicknameIgnoreCase(request.nickname())) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "Nickname is already taken");
        }
        User user = new User();
        user.setNickname(request.nickname());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return toResponse(user, authSessionService.openSession(user));
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository
                .findByNicknameIgnoreCase(request.nickname())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "Invalid nickname or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "Invalid nickname or password");
        }
        return toResponse(user, authSessionService.openSession(user));
    }

    private AuthResponse toResponse(User user, IssuedToken issued) {
        return new AuthResponse(
                issued.token(),
                new UserDto(user.getId(), user.getNickname(), sessionRegistry.isOnline(user.getId())));
    }
}
