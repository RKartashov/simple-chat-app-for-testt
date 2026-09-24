package com.simplechat.domain.repository;

import com.simplechat.domain.entity.AuthSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    @Query("""
        select s from AuthSession s
        join fetch s.user
        where s.tokenJti = :jti and s.expiresAt > :now
        """)
    Optional<AuthSession> findActiveByTokenJti(@Param("jti") String jti, @Param("now") Instant now);

}
