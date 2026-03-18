package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.conversation.ConversationResponse;
import com.a9.etutoring.domain.dto.conversation.CreateConversationRequest;
import com.a9.etutoring.domain.dto.conversation.CreateConversationResult;
import com.a9.etutoring.domain.dto.message.MessageCreateRequest;
import com.a9.etutoring.domain.dto.message.MessageResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConversationService {

    CreateConversationResult create(UUID currentUserId, CreateConversationRequest request);

    Page<ConversationResponse> list(UUID currentUserId, Pageable pageable);

    ConversationResponse getById(UUID currentUserId, UUID conversationId);

    Page<MessageResponse> listMessages(UUID currentUserId, UUID conversationId, Pageable pageable);

    MessageResponse sendMessage(UUID currentUserId, UUID conversationId, MessageCreateRequest request);

    void markConversationRead(UUID currentUserId, UUID conversationId);
}
