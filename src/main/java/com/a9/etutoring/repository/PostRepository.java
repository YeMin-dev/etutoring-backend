package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Post;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query(
        """
        select p
        from Post p
        where p.createdBy.deletedDate is null
        order by p.createdDate desc
        """
    )
    List<Post> findAllVisibleForStaff();

    @Query(
        """
        select distinct p
        from Post p
        left join p.targets t
        where p.createdBy.deletedDate is null
          and (t.student.id = :studentId or t is null)
        order by p.createdDate desc
        """
    )
    List<Post> findVisibleForStudent(@Param("studentId") UUID studentId);
}
