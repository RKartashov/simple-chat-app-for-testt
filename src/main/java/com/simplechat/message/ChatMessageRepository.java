package com.simplechat.message;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m from ChatMessage m
            join fetch m.sender
            join fetch m.recipient
            where (m.sender.id = :leftUserId and m.recipient.id = :rightUserId)
               or (m.sender.id = :rightUserId and m.recipient.id = :leftUserId)
            order by m.createdAt asc, m.id asc
            """)
    List<ChatMessage> findConversation(
            @Param("leftUserId") Long leftUserId, @Param("rightUserId") Long rightUserId);

    @Query("""
            select m from ChatMessage m
            join fetch m.sender
            join fetch m.recipient
            where m.recipient.id = :recipientId and m.status = :status
            order by m.createdAt asc, m.id asc
            """)
    List<ChatMessage> findPendingForRecipient(
            @Param("recipientId") Long recipientId, @Param("status") MessageStatus status);

    @Query("""
            select m from ChatMessage m
            join fetch m.sender
            join fetch m.recipient
            where m.sender.id = :senderId
              and m.recipient.id = :recipientId
              and m.status in :statuses
            """)
    List<ChatMessage> findIncomingWithStatuses(
            @Param("senderId") Long senderId,
            @Param("recipientId") Long recipientId,
            @Param("statuses") Collection<MessageStatus> statuses);
}
