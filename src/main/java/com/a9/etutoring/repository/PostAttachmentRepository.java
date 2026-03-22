package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.PostAttachment;
import com.a9.etutoring.repository.projection.PostAttachmentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, UUID> {

    List<PostAttachment> findAllByPost_Id(UUID postId);

    List<PostAttachmentSummary> findProjectedByPost_Id(UUID postId);

    Optional<PostAttachment> findByIdAndPost_Id(UUID id, UUID postId);

    void deleteAllByPost_Id(UUID postId);

    void deleteAllByIdInAndPost_Id(List<UUID> ids, UUID postId);
}
