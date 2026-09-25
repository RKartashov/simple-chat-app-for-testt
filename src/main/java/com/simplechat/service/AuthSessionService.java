package com.simplechat.service;

import com.simplechat.common.EntityService;
import com.simplechat.domain.entity.AuthSession;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.AuthSessionRepository;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.security.JwtService;
import com.simplechat.security.JwtService.IssuedToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthSessionService implements EntityService<AuthSession, Long> {

    @Getter
    private final AuthSessionRepository repository;
    private final JwtService jwtService;

    public IssuedToken openSession(User user) {
        IssuedToken issued = jwtService.issueToken(user.getId(), user.getNickname());
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenJti(issued.jti());
        session.setCreatedAt(issued.issuedAt());
        session.setExpiresAt(issued.expiresAt());
        save(session);

        return issued;
    }

    @Transactional(readOnly = true)
    public AuthPrincipal resolvePrincipal(String token) {
        Claims claims = jwtService.parse(token);
        String jti = claims.getId();
        AuthSession session = repository.findActiveByTokenJti(jti, Instant.now())
                                        .orElseThrow(() -> new JwtException("Session is not active"));
        User user = session.getUser();

        return new AuthPrincipal(user.getId(), user.getNickname(), jti);
    }

}
