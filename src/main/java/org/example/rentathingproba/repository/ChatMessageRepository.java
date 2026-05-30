package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    long countByConversationIdAndReadFalseAndSenderIdNot(Long conversationId, Long senderId);
    List<ChatMessage> findByConversationIdAndTypeNotOrderBySentAtAsc(Long conversationId, ChatMessage.MessageType type);

    @Modifying
    @Query("""
        UPDATE ChatMessage m
        SET m.read = true
        WHERE m.conversation.id = :convId
        AND m.sender.id != :readerId
        AND m.read = false
    """)
    void markAllReadInConversation(@Param("convId") Long conversationId,
                                   @Param("readerId") Long readerId);
}