package com.a9.etutoring.exception;

import com.a9.etutoring.domain.dto.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value"))
            .findFirst()
            .orElse("Request validation failed");
        log.warn("Validation failed: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Not found: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateResourceException ex) {
        log.warn("Conflict: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthRuntime(Exception ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("AUTHENTICATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        String message = Optional.ofNullable(ex.getMostSpecificCause())
            .map(Throwable::getMessage)
            .filter(m -> m != null && !m.isBlank())
            .orElse("One or more files exceed the maximum permitted size");
        log.warn("File size limit exceeded: {}", message);
        return ResponseEntity.badRequest().body(new ErrorResponse("FILE_TOO_LARGE", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getMostSpecificCause();
        String rootMessage = rootCause != null ? rootCause.getMessage() : "";

        if (rootMessage != null && rootMessage.contains("ux_users_username")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_USERNAME", "Username already exists"));
        }
        if (rootMessage != null && rootMessage.contains("ux_users_email")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_EMAIL", "Email already exists"));
        }

        if (ex.getCause() instanceof ConstraintViolationException constraintViolationException) {
            String constraint = constraintViolationException.getConstraintName();
            if ("ux_users_username".equals(constraint)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("DUPLICATE_USERNAME", "Username already exists"));
            }
            if ("ux_users_email".equals(constraint)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("DUPLICATE_EMAIL", "Email already exists"));
            }
        }

        log.warn("Data integrity violation: {}", rootMessage);
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("DATA_INTEGRITY_ERROR", "Request violates data integrity rules"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", "The requested resource was not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(new ErrorResponse("METHOD_NOT_ALLOWED", "Request method '" + ex.getMethod() + "' is not supported"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        String fileSizeMessage = findFileSizeLimitMessage(ex);
        if (fileSizeMessage != null) {
            log.warn("File size limit exceeded: {}", fileSizeMessage);
            return ResponseEntity.badRequest().body(new ErrorResponse("FILE_TOO_LARGE", fileSizeMessage));
        }
        log.error("Unexpected error: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred"));
    }

    private static String findFileSizeLimitMessage(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("exceeds its maximum permitted size")) {
                return msg;
            }
        }
        return null;
    }
}
