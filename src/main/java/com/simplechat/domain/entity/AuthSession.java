package com.simplechat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Сессии подключений пользователей
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Токен
     */
    @Column(name = "token_jti", nullable = false, unique = true, length = 64)
    private String tokenJti;

    /**
     * Время начала сессии
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Время протухания сессии
     */
    @Column(nullable = false)
    private Instant expiresAt;

}
