package com.simplechat.domain.repository;

import com.simplechat.domain.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);

    @Query("""
        select m from ChatMessage m
        join fetch m.sender
        where m.recipient.id = :recipientId and m.status = 'SENT'
        order by m.createdAt asc, m.id asc
        """)
    List<ChatMessage> findPendingForRecipient(@Param("recipientId") Long recipientId);

    @Query("""
        select m from ChatMessage m
        join fetch m.sender
        where m.conversation.id = :conversationId
          and m.recipient.id = :recipientId
          and m.status <> 'READ'
        """)
    List<ChatMessage> findUnreadInConversation(
        @Param("conversationId") Long conversationId,
        @Param("recipientId") Long recipientId
    );

}
