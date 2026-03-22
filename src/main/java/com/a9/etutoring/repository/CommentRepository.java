package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query(
        """
        select c
        from Comment c
        where c.post.id = :postId
          and c.authorUser.deletedDate is null
        order by c.createdDate asc
        """
    )
    List<Comment> findAllVisibleByPostIdOrderByCreatedDateAsc(@Param("postId") UUID postId);
}
