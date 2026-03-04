package com.a9.etutoring.service;

import java.util.UUID;

public interface AllocationService {

    void allocateTutor(UUID studentId, UUID tutorId);
}