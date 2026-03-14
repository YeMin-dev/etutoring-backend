package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.TutorAllocation;
import com.a9.etutoring.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TutorAllocationRepository extends JpaRepository<TutorAllocation, UUID> {

    @Query("""
        SELECT DISTINCT ta.student FROM TutorAllocation ta
        WHERE ta.tutor.id = :tutorId AND ta.endedDate IS NULL
        AND (ta.scheduleEnd IS NULL OR ta.scheduleEnd >= CURRENT_TIMESTAMP)
        ORDER BY ta.student.username
        """)
    List<User> findDistinctStudentsByTutorIdAndEndedDateIsNull(@Param("tutorId") UUID tutorId);

    @Query("""
        SELECT ta FROM TutorAllocation ta
        WHERE ta.tutor.id = :tutorId AND ta.endedDate IS NULL
        AND ta.scheduleStart IS NOT NULL AND ta.scheduleEnd IS NOT NULL
        AND :scheduleEnd > ta.scheduleStart AND :scheduleStart < ta.scheduleEnd
        """)
    List<TutorAllocation> findActiveByTutorWithScheduleOverlap(
        @Param("tutorId") UUID tutorId,
        @Param("scheduleStart") Instant scheduleStart,
        @Param("scheduleEnd") Instant scheduleEnd
    );

    @Query("""
        SELECT ta FROM TutorAllocation ta
        WHERE ta.tutor.id = :tutorId AND ta.endedDate IS NULL
        AND ta.id <> :exclusionAllocationId
        AND ta.scheduleStart IS NOT NULL AND ta.scheduleEnd IS NOT NULL
        AND :scheduleEnd > ta.scheduleStart AND :scheduleStart < ta.scheduleEnd
        """)
    List<TutorAllocation> findActiveByTutorWithScheduleOverlapExcluding(
        @Param("tutorId") UUID tutorId,
        @Param("scheduleStart") Instant scheduleStart,
        @Param("scheduleEnd") Instant scheduleEnd,
        @Param("exclusionAllocationId") UUID exclusionAllocationId
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"student", "tutor", "allocatedBy"})
    @Query("""
        SELECT ta FROM TutorAllocation ta
        WHERE ta.endedDate IS NULL
        AND (:search IS NULL OR :search = '' OR
            LOWER(CONCAT(CONCAT(ta.student.firstName, ' '), ta.student.lastName)) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))
            OR LOWER(CONCAT(CONCAT(ta.tutor.firstName, ' '), ta.tutor.lastName)) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')))
        """)
    Page<TutorAllocation> findAllBySearch(@Param("search") String search, Pageable pageable);
}
