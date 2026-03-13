package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.PostAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, UUID> {

    List<PostAttachment> findAllByPost_Id(UUID postId);

    Optional<PostAttachment> findByIdAndPost_Id(UUID id, UUID postId);
}
