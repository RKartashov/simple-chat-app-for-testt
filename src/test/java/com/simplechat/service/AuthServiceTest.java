package com.simplechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simplechat.domain.entity.User;
import com.simplechat.exception.ApiException;
import com.simplechat.rest.dto.AuthRequest;
import com.simplechat.rest.dto.AuthResponse;
import com.simplechat.security.JwtService.IssuedToken;
import com.simplechat.websocket.WebSocketSessionRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    UserService userService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthSessionService authSessionService;

    @Mock
    WebSocketSessionRegistry sessionRegistry;

    @InjectMocks
    AuthService authService;

    @Test
    void registersUser() {
        when(userService.existsByNickname("testNick")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("PasswordHash");
        when(userService.createUser("testNick", "PasswordHash"))
            .thenAnswer(inv -> {
                User user = new User();
                user.setNickname(inv.getArgument(0));
                user.setPasswordHash(inv.getArgument(1));
                user.setId(1L);
                return user;
            });
        when(authSessionService.openSession(any(User.class))).thenReturn(getFixedToken());
        when(sessionRegistry.isUserOnline(1L)).thenReturn(true);

        AuthRequest request = new AuthRequest("testNick", "12345678");
        AuthResponse response = authService.register(request);

        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().nickname()).isEqualTo("testNick");
        assertThat(response.token()).isEqualTo("jwt.token.value");

        verify(userService).createUser("testNick", "PasswordHash");
        verify(passwordEncoder).encode("12345678");
        verify(userService, never()).createUser(any(), eq("12345678"));
    }

    @Test
    void throwsOnRegisterExistingUser() {
        when(userService.existsByNickname("existingNick")).thenReturn(true);

        AuthRequest request = new AuthRequest("existingNick", "12345678");
        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ApiException.class)
            .hasMessage("Nickname is already taken");
    }

    @Test
    void loginsUser() {
        when(userService.findByNickname("existingNick")).thenReturn(Optional.of(getExistingUser()));
        when(passwordEncoder.matches("12345678", "PasswordHash")).thenReturn(true);
        when(authSessionService.openSession(any(User.class))).thenReturn(getFixedToken());
        when(sessionRegistry.isUserOnline(1L)).thenReturn(true);

        AuthRequest request = new AuthRequest("existingNick", "12345678");
        AuthResponse response = authService.login(request);

        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().nickname()).isEqualTo("existingNick");
        assertThat(response.token()).isEqualTo("jwt.token.value");

        verify(userService).findByNickname("existingNick");
        verify(passwordEncoder).matches("12345678", "PasswordHash");
        verify(userService, never()).createUser(any(), eq("12345678"));
    }

    @Test
    void throwsOnLoginNonexistentUser() {
        when(userService.findByNickname("unexistingNick")).thenReturn(Optional.empty());

        AuthRequest request = new AuthRequest("unexistingNick", "12345678");
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(ApiException.class)
            .hasMessage("Invalid nickname or password");

        verify(userService).findByNickname("unexistingNick");
        verify(passwordEncoder, never()).matches(eq("12345678"), any());
        verify(authSessionService, never()).openSession(any(User.class));
    }

    @Test
    void throwsOnLoginWithWrongPassword() {
        when(userService.findByNickname("existingNick")).thenReturn(Optional.of(getExistingUser()));
        when(passwordEncoder.matches("wrong", "PasswordHash")).thenReturn(false);

        AuthRequest request = new AuthRequest("existingNick", "wrong");
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(ApiException.class)
            .hasMessage("Invalid nickname or password");

        verify(userService).findByNickname("existingNick");
        verify(passwordEncoder).matches("wrong", "PasswordHash");
        verify(authSessionService, never()).openSession(any(User.class));
    }

    IssuedToken getFixedToken() {
        return new IssuedToken(
            "jwt.token.value",
            "test-jti-123",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2028-01-01T01:00:00Z")
        );
    }

    User getExistingUser() {
        User user = new User();
        user.setId(1L);
        user.setNickname("existingNick");
        user.setPasswordHash("PasswordHash");
        return user;
    }

}
