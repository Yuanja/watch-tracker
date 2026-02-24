package com.tradeintel.common.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handler that converts exceptions into RFC 9457
 * {@link ProblemDetail} JSON responses.
 *
 * <p>Handled exception types:
 * <ul>
 *   <li>{@link ResourceNotFoundException}      → 404 Not Found</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request (with field errors)</li>
 *   <li>{@link IllegalArgumentException}        → 400 Bad Request</li>
 *   <li>{@link AuthenticationException}         → 401 Unauthorized</li>
 *   <li>{@link AccessDeniedException}           → 403 Forbidden</li>
 *   <li>{@link Exception}                       → 500 Internal Server Error</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // 404 — Resource Not Found
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:tradeintel:error:not-found"));
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // -------------------------------------------------------------------------
    // 400 — Validation errors from @Valid / @Validated
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:tradeintel:error:validation-failed"));
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more request fields are invalid.");
        problem.setProperty("fieldErrors", fieldErrors);
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // -------------------------------------------------------------------------
    // 400 — Illegal argument
    // -------------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:tradeintel:error:bad-request"));
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // -------------------------------------------------------------------------
    // 401 — Authentication required
    // -------------------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("Authentication failure: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create("urn:tradeintel:error:unauthorized"));
        problem.setTitle("Unauthorized");
        problem.setDetail("Authentication is required to access this resource.");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    // -------------------------------------------------------------------------
    // 403 — Access denied
    // -------------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setType(URI.create("urn:tradeintel:error:forbidden"));
        problem.setTitle("Forbidden");
        problem.setDetail("You do not have permission to access this resource.");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    // -------------------------------------------------------------------------
    // 500 — Unhandled server error (catch-all)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception", ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("urn:tradeintel:error:internal"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again later.");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
