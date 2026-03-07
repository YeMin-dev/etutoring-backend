package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.Allocation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, UUID> {

    Optional<Allocation> findByStudentIdAndDeletedDateIsNull(UUID studentId);
}