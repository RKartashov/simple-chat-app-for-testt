package com.simplechat.session;

import com.simplechat.security.AuthPrincipal;
import com.simplechat.security.JwtService;
import com.simplechat.security.JwtService.IssuedToken;
import com.simplechat.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        try {
            Claims claims = jwtService.parse(token);
            String jti = claims.getId();
            AuthSession session = authSessionRepository
                    .findActiveByTokenJti(jti, Instant.now())
                    .orElseThrow(() -> new JwtException("Session is not active"));
            User user = session.getUser();
            return new AuthPrincipal(user.getId(), user.getNickname(), jti);
        } catch (JwtException | IllegalArgumentException ex) {
            throw ex;
        }
    }
}
