package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.PostTarget;
import com.a9.etutoring.domain.model.PostTargetId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTargetRepository extends JpaRepository<PostTarget, PostTargetId> {

    boolean existsByPost_IdAndStudent_Id(UUID postId, UUID studentId);

    boolean existsByPost_Id(UUID postId);

    void deleteAllByPost_Id(UUID postId);

    List<PostTarget> findAllByPost_Id(UUID postId);
}
