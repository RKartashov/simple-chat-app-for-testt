package com.simplechat.service;

import com.simplechat.domain.entity.AuthSession;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.AuthSessionRepository;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.security.JwtService;
import com.simplechat.security.JwtService.IssuedToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final JwtService jwtService;

    @Transactional
    public IssuedToken openSession(User user) {
        IssuedToken issued = jwtService.issueToken(user.getId(), user.getNickname());
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenJti(issued.jti());
        session.setCreatedAt(issued.issuedAt());
        session.setExpiresAt(issued.expiresAt());
        authSessionRepository.save(session);
        return issued;
    }

    @Transactional(readOnly = true)
    public AuthPrincipal resolvePrincipal(String token) {
        Claims claims = jwtService.parse(token);
        String jti = claims.getId();
        AuthSession session = authSessionRepository
                .findActiveByTokenJti(jti, Instant.now())
                .orElseThrow(() -> new JwtException("Session is not active"));
        User user = session.getUser();
        return new AuthPrincipal(user.getId(), user.getNickname(), jti);
    }
}
