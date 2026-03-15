package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.conversation.ConversationResponse;
import com.a9.etutoring.domain.dto.conversation.CreateConversationRequest;
import com.a9.etutoring.domain.dto.conversation.CreateConversationResult;
import com.a9.etutoring.domain.dto.message.MessageCreateRequest;
import com.a9.etutoring.domain.dto.message.MessageResponse;
import com.a9.etutoring.domain.model.Conversation;
import com.a9.etutoring.domain.model.Message;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.ForbiddenException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.ConversationRepository;
import com.a9.etutoring.repository.MessageRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.ConversationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository,
                                   UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CreateConversationResult create(UUID currentUserId, CreateConversationRequest request) {
        UUID studentId = request.studentUserId();
        UUID tutorId = request.tutorUserId();
        if (!currentUserId.equals(studentId) && !currentUserId.equals(tutorId)) {
            throw new ForbiddenException("NOT_PARTICIPANT", "You must be the tutor or student of this conversation");
        }
        User student = userRepository.findByIdAndDeletedDateIsNull(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Student not found"));
        User tutor = userRepository.findByIdAndDeletedDateIsNull(tutorId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Tutor not found"));
        Conversation existing = conversationRepository.findByStudent_IdAndTutor_Id(studentId, tutorId).orElse(null);
        if (existing != null) {
            return new CreateConversationResult(false, toConversationResponse(existing));
        }
        Conversation c = new Conversation();
        c.setId(UUID.randomUUID());
        c.setStudent(student);
        c.setTutor(tutor);
        c.setCreatedDate(Instant.now());
        Conversation saved = conversationRepository.save(c);
        return new CreateConversationResult(true, toConversationResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> list(UUID currentUserId, Pageable pageable) {
        Page<Conversation> page = conversationRepository.findByParticipantOrderByCreatedDateDesc(currentUserId, pageable);
        List<ConversationResponse> content = page.getContent().stream().map(this::toConversationResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getById(UUID currentUserId, UUID conversationId) {
        Conversation c = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
        ensureParticipant(c, currentUserId);
        return toConversationResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> listMessages(UUID currentUserId, UUID conversationId, Pageable pageable) {
        Conversation c = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
        ensureParticipant(c, currentUserId);
        Page<Message> page = messageRepository.findByConversation_IdOrderByCreatedDateAsc(conversationId, pageable);
        List<MessageResponse> content = page.getContent().stream().map(this::toMessageResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    public MessageResponse sendMessage(UUID currentUserId, UUID conversationId, MessageCreateRequest request) {
        Conversation c = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
        ensureParticipant(c, currentUserId);
        User sender = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        Message m = new Message();
        m.setId(UUID.randomUUID());
        m.setConversation(c);
        m.setSender(sender);
        m.setBody(request.body().trim());
        m.setCreatedDate(Instant.now());
        m.setReadDate(null);
        Message saved = messageRepository.save(m);
        return toMessageResponse(saved);
    }

    @Override
    public void markConversationRead(UUID currentUserId, UUID conversationId) {
        Conversation c = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("CONVERSATION_NOT_FOUND", "Conversation not found"));
        ensureParticipant(c, currentUserId);
        UUID senderUserId = c.getStudent().getId().equals(currentUserId) ? c.getTutor().getId() : c.getStudent().getId();
        messageRepository.setReadDateByConversationAndSender(conversationId, senderUserId, Instant.now());
    }

    private void ensureParticipant(Conversation c, UUID userId) {
        boolean participant = c.getStudent().getId().equals(userId) || c.getTutor().getId().equals(userId);
        if (!participant) {
            throw new ForbiddenException("NOT_PARTICIPANT", "Not a participant of this conversation");
        }
    }

    private ConversationResponse toConversationResponse(Conversation c) {
        return new ConversationResponse(
            c.getId(),
            c.getStudent().getId(),
            c.getTutor().getId(),
            c.getCreatedDate()
        );
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(
            m.getId(),
            m.getConversation().getId(),
            m.getSender().getId(),
            m.getBody(),
            m.getCreatedDate(),
            m.getReadDate()
        );
    }
}
