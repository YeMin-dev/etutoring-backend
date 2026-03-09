package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Meeting;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    Page<Meeting> findByTutor_IdOrderByStartDateDesc(UUID tutorId, Pageable pageable);

    Optional<Meeting> findByIdAndTutor_Id(UUID id, UUID tutorId);
}
