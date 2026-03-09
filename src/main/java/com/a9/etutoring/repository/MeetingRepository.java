package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Meeting;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Page<Meeting> findByTutor_IdOrderByStartDateDesc(UUID tutorId, Pageable pageable);

    Optional<Meeting> findByIdAndTutor_Id(UUID id, UUID tutorId);

    @Query("""
        SELECT m FROM Meeting m
        WHERE m.tutor.id = :tutorId
        AND :endDate > m.startDate AND :startDate < m.endDate
        """)
    List<Meeting> findByTutorWithScheduleOverlap(
        @Param("tutorId") UUID tutorId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    @Query("""
        SELECT m FROM Meeting m
        WHERE m.tutor.id = :tutorId AND m.id <> :exclusionMeetingId
        AND :endDate > m.startDate AND :startDate < m.endDate
        """)
    List<Meeting> findByTutorWithScheduleOverlapExcluding(
        @Param("tutorId") UUID tutorId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("exclusionMeetingId") UUID exclusionMeetingId
    );
}
