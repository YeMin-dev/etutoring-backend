package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @EntityGraph(attributePaths = {"student", "tutor"})
    Optional<Conversation> findById(UUID id);

    Optional<Conversation> findByStudent_IdAndTutor_Id(UUID studentId, UUID tutorId);

    @Query("SELECT c FROM Conversation c WHERE c.student.id = :userId OR c.tutor.id = :userId ORDER BY c.createdDate DESC")
    Page<Conversation> findByParticipantOrderByCreatedDateDesc(@Param("userId") UUID userId, Pageable pageable);
}
