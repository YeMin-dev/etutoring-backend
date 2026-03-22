package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.conversation.ConversationResponse;
import com.a9.etutoring.domain.dto.conversation.CreateConversationRequest;
import com.a9.etutoring.domain.dto.conversation.CreateConversationResult;
import com.a9.etutoring.domain.dto.message.MessageCreateRequest;
import com.a9.etutoring.domain.dto.message.MessageResponse;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.ConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @PostMapping
    public ResponseEntity<ConversationResponse> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateConversationRequest request
    ) {
        CreateConversationResult result = conversationService.create(requirePrincipal(principal), request);
        return result.created()
            ? ResponseEntity.status(HttpStatus.CREATED).body(result.conversation())
            : ResponseEntity.ok(result.conversation());
    }

    @GetMapping
    public Page<ConversationResponse> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = requirePrincipal(principal);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdDate"));
        return conversationService.list(currentUserId, pageable);
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getById(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID conversationId
    ) {
        return conversationService.getById(requirePrincipal(principal), conversationId);
    }

    @GetMapping("/{conversationId}/messages")
    public Page<MessageResponse> listMessages(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID conversationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        UUID currentUserId = requirePrincipal(principal);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.ASC, "createdDate"));
        return conversationService.listMessages(currentUserId, conversationId, pageable);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID conversationId,
        @Valid @RequestBody MessageCreateRequest request
    ) {
        return conversationService.sendMessage(requirePrincipal(principal), conversationId, request);
    }

    @PatchMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markConversationRead(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID conversationId
    ) {
        conversationService.markConversationRead(requirePrincipal(principal), conversationId);
    }

    private static UUID requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication required");
        }
        return principal.getId();
    }
}
