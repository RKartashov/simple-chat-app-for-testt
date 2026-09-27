package com.simplechat.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Диалог - уникальная пара из двух пользователей, открывших чат между собой хоть раз.
 * Нужен для удобного поиска сообщений между ними.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "low_user_id", nullable = false)
    private User lowIdUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "high_user_id", nullable = false)
    private User highIdUser;

}
