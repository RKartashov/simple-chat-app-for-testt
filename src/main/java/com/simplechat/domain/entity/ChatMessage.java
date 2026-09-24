package com.simplechat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Сообщения пользователей в чатах
 */
@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_messages_sender_recipient_created", columnList = "sender_id, recipient_id, created_at"),
        @Index(name = "idx_messages_recipient_sender_created", columnList = "recipient_id, sender_id, created_at"),

        // Здесь лучше использовать partial индекс по status = 'SENT', используется для поиска неполученных сообщений
        // и обновления их статуса. Это нельзя написать через аннотацию тут и нужен liquibase или другой мигратор
        // (я не буду для такого мини приложения добавлять liquibase :))
        @Index(name = "idx_messages_recipient_status", columnList = "recipient_id, status")
    })
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь-отправитель сообщения
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Пользователь-получатель сообщения
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * Текст сообщения
     */
    @Column(nullable = false, length = 4000)
    private String text;

    /**
     * Статус сообщения - отправлено, доставлено, прочитано
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatMessageStatus status;

    /**
     * Время отправки сообщения
     */
    @Column(nullable = false)
    private Instant createdAt;

}
