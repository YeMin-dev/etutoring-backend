package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Message;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversation_IdOrderByCreatedDateAsc(UUID conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.readDate = :readDate WHERE m.conversation.id = :conversationId AND m.sender.id = :senderId AND m.readDate IS NULL")
    int setReadDateByConversationAndSender(
        @Param("conversationId") UUID conversationId,
        @Param("senderId") UUID senderId,
        @Param("readDate") Instant readDate
    );
}
