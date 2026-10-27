package com.iitsm.web.error;

import com.iitsm.domain.exception.DuplicateIncidentException;
import com.iitsm.domain.exception.IitsmException;
import com.iitsm.domain.exception.InvalidCredentialsException;
import com.iitsm.domain.exception.InvalidStateTransitionException;
import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.exception.ServiceNotFoundException;
import com.iitsm.domain.exception.TechnicianOverloadedException;
import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handler mapping all IitsmException hierarchy instances
 * and standard web validation failures into uniform, RFC-compliant JSON error representations.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record FieldErrorDetail(String field, String message) {}

    public record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            List<FieldErrorDetail> fieldErrors) {}

    @ExceptionHandler(IitsmException.class)
    public ResponseEntity<ErrorResponse> handleIitsmException(IitsmException ex) {
        HttpStatus status = resolveStatus(ex);
        log.warn("Domain exception caught [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getErrorCode(),
                ex.getMessage(),
                Collections.emptyList());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "ERR_MALFORMED_JSON",
                "Malformed request body or unparseable JSON payload",
                Collections.emptyList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldErrorDetail(f.getField(), f.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "ERR_VALIDATION_FAILED",
                "Input validation failed for one or more fields",
                errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "ERR_FORBIDDEN",
                ex.getMessage(),
                Collections.emptyList());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "ERR_UNAUTHORIZED",
                ex.getMessage(),
                Collections.emptyList());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(java.util.ConcurrentModificationException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentModification(java.util.ConcurrentModificationException ex) {
        log.warn("Concurrent modification caught: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "ERR_CONCURRENT_MODIFICATION",
                "The resource was modified concurrently by another transaction. Please reload and retry.",
                Collections.emptyList());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "ERR_INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred",
                Collections.emptyList());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus resolveStatus(IitsmException ex) {
        if (ex instanceof UnauthorizedOperationException) {
            return HttpStatus.FORBIDDEN;
        } else if (ex instanceof InvalidCredentialsException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ServiceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (ex instanceof InvalidStateTransitionException
                || ex instanceof DuplicateIncidentException
                || ex instanceof TechnicianOverloadedException
                || ex instanceof TechnicianUnavailableException) {
            return HttpStatus.CONFLICT;
        } else if (ex instanceof PersistenceFailureException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            return HttpStatus.BAD_REQUEST;
        }
    }
}

