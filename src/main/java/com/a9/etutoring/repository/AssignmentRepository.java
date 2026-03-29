package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Assignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByCreatedBy_IdAndDeletedDateIsNullOrderByCreatedDateDesc(UUID createdById);

    Optional<Assignment> findByIdAndDeletedDateIsNull(UUID id);

    @Query(
        """
        SELECT DISTINCT a
        FROM Assignment a, TutorAllocation ta
        WHERE a.createdBy.id = ta.tutor.id
          AND ta.student.id = :studentId
          AND a.deletedDate IS NULL
          AND a.createdBy.deletedDate IS NULL
          AND ta.endedDate IS NULL
          AND (ta.scheduleEnd IS NULL OR ta.scheduleEnd >= CURRENT_TIMESTAMP)
        ORDER BY a.createdDate DESC
        """
    )
    List<Assignment> findVisibleForStudent(@Param("studentId") UUID studentId);
}
