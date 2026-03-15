package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.meeting.MeetingCreateRequest;
import com.a9.etutoring.domain.dto.meeting.MeetingResponse;
import com.a9.etutoring.domain.dto.meeting.MeetingUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingService {

    MeetingResponse create(UUID currentUserId, MeetingCreateRequest request);

    Page<MeetingResponse> list(UUID currentUserId, Pageable pageable);

    Page<MeetingResponse> listForStudent(UUID currentUserId, Pageable pageable);

    MeetingResponse getById(UUID currentUserId, UUID id);

    MeetingResponse getByIdForStudent(UUID currentUserId, UUID id);

    MeetingResponse update(UUID currentUserId, UUID id, MeetingUpdateRequest request);

    void delete(UUID currentUserId, UUID id);
}
