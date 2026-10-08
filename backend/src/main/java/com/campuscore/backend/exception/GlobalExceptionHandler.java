package com.campuscore.backend.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.WebUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions into a single, predictable error envelope.
 *
 * <p>Every response body has the same four keys:
 * <pre>
 *   { "status": 409, "error": "Conflict", "message": "...", "errors": { "field": "..." } }
 * </pre>
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>Request validation problems are 400.</li>
 *   <li>Relationship/dependency problems (foreign keys, unique constraints,
 *       concurrent modification) are 409, never 500.</li>
 *   <li>Framework-level client errors keep their own status (404, 405, 406, 415).</li>
 *   <li>Responses never echo exception messages for unexpected failures, and never
 *       include a stack trace, SQL, entity state, or exception class names. Anything
 *       worth investigating is logged server-side instead.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String GENERIC_SERVER_ERROR =
            "The server could not complete this request. Please try again later.";

    static final String GENERIC_CLIENT_ERROR =
            "The request could not be completed. Check the details and try again.";

    static final String DEPENDENCY_CONFLICT =
            "This record is referenced by other records and cannot be changed or removed.";

    static final String CONCURRENT_UPDATE =
            "The record was updated by another transaction. Please reload and try again.";

    // ------------------------------------------------------------------
    // Domain exceptions
    // ------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException exception) {

        String message = safeMessage(exception.getMessage(), GENERIC_CLIENT_ERROR);

        return build(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleResourceConflictException(
            ResourceConflictException exception) {

        String message = safeMessage(exception.getMessage(), DEPENDENCY_CONFLICT);

        return build(HttpStatus.CONFLICT, message);
    }

    /**
     * Raised when the database refuses a write because a foreign key or unique
     * constraint would be violated. Reported as 409 so callers can distinguish
     * "your request conflicts with existing data" from a server fault, and the
     * database message is discarded so SQL and column names stay internal.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {

        log.warn("Data integrity violation rejected request", exception);

        return build(HttpStatus.CONFLICT, DEPENDENCY_CONFLICT);
    }

    /**
     * Optimistic (and pessimistic) locking failures mean a concurrent writer won
     * the race, which is a conflict with the caller's expectations, not a fault.
     */
    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(
            Exception exception) {

        log.warn("Concurrent modification rejected request", exception);

        return build(HttpStatus.CONFLICT, CONCURRENT_UPDATE);
    }

    /**
     * A constraint failure raised while a transaction was committing is wrapped by
     * Spring, so unwrap it to recover the 409 that the underlying cause deserves.
     */
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionException(
            TransactionException exception) {

        if (isDataIntegrityFailure(exception)) {
            log.warn("Data integrity violation rejected request at transaction commit", exception);
            return build(HttpStatus.CONFLICT, DEPENDENCY_CONFLICT);
        }

        log.error("Transaction failure while handling request", exception);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_SERVER_ERROR);
    }

    /**
     * Preserve the status a service deliberately chose (for example invalid
     * pagination parameters or "profile not linked to this account").
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException exception) {

        HttpStatusCode status = exception.getStatusCode();

        String message = status.is5xxServerError()
                ? GENERIC_SERVER_ERROR
                : safeMessage(exception.getReason(), GENERIC_CLIENT_ERROR);

        if (status.is5xxServerError()) {
            log.error("Request failed with status {}", status.value(), exception);
        }

        return build(status, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException exception) {

        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(
                        violation.getPropertyPath().toString(),
                        safeMessage(violation.getMessage(), GENERIC_CLIENT_ERROR))
        );

        return build(HttpStatus.BAD_REQUEST, "Constraint validation failed", errors);
    }

    /**
     * Services use this for malformed arguments (for example linking a STUDENT
     * account to a faculty profile), which is a client mistake, so 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        String message = safeMessage(exception.getMessage(), GENERIC_CLIENT_ERROR);

        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Backstop. Without this, an unexpected exception is rendered by the servlet
     * container in whatever shape it likes; with it, callers always receive the
     * same envelope and never see exception messages or stack traces.
     *
     * <p>Framework exceptions that arrive wrapped in another exception are
     * unwrapped first, so a rethrown client error keeps its 4xx status instead of
     * being reported as a server fault.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception exception) {

        ErrorResponse wrapped = findWrappedErrorResponse(exception);
        if (wrapped != null && !wrapped.getStatusCode().is5xxServerError()) {

            HttpStatusCode status = wrapped.getStatusCode();
            String detail = safeMessage(wrapped.getBody().getDetail(), GENERIC_CLIENT_ERROR);

            log.debug("Request rejected with status {} (wrapped {})",
                    status.value(), exception.getClass().getSimpleName());

            return build(status, detail);
        }

        log.error("Unhandled exception while processing request", exception);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_SERVER_ERROR);
    }

    // ------------------------------------------------------------------
    // Standard Spring MVC exceptions
    //
    // Extending ResponseEntityExceptionHandler keeps the framework's own status
    // mapping (404 / 405 / 406 / 415 / 400 ...) intact while the single override
    // below normalises the body.
    // ------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(),
                        safeMessage(error.getDefaultMessage(), GENERIC_CLIENT_ERROR))
        );
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                errors.putIfAbsent(error.getObjectName(),
                        safeMessage(error.getDefaultMessage(), GENERIC_CLIENT_ERROR))
        );

        log.debug("Rejected request with {} field validation error(s)", errors.size());

        return new ResponseEntity<>(envelope(status, "Validation failed", errors), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        if (request instanceof ServletWebRequest servletWebRequest
                && servletWebRequest.getResponse() != null
                && servletWebRequest.getResponse().isCommitted()) {

            log.warn("Response already committed; cannot report status {}",
                    statusCode.value(), ex);
            return null;
        }

        String message = describe(ex, statusCode);

        if (statusCode.is5xxServerError()) {
            // Keep the exception reachable for container-level error handling.
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
            log.error("Request failed with status {}", statusCode.value(), ex);
        } else {
            log.debug("Request rejected with status {} ({})",
                    statusCode.value(), ex.getClass().getSimpleName());
        }

        return new ResponseEntity<>(envelope(statusCode, message, Map.of()), headers, statusCode);
    }

    /**
     * Produces a safe, human-readable reason for a framework-level failure.
     *
     * <p>Messages are written by the application or by the framework's own
     * message catalog, never taken verbatim from {@code getMessage()}: several of
     * these exceptions embed the rejected request payload, converter internals, or
     * fully qualified class names in their message, which must not reach a client.
     */
    private String describe(Exception ex, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return GENERIC_SERVER_ERROR;
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return "This endpoint does not support the HTTP method used.";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "The request content type is not supported by this endpoint.";
        }
        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            return "This endpoint cannot produce the requested content type.";
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Request body is missing or malformed.";
        }
        if (ex instanceof MissingServletRequestParameterException parameterException) {
            return "Required request parameter '"
                    + safeSegment(parameterException.getParameterName())
                    + "' is missing or invalid.";
        }
        if (ex instanceof TypeMismatchException) {
            return "A request parameter has an unexpected type or value.";
        }
        if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
            return "The requested endpoint was not found.";
        }
        if (ex instanceof ErrorResponseException errorResponseException) {
            String detail = errorResponseException.getBody().getDetail();
            return safeMessage(detail, GENERIC_CLIENT_ERROR);
        }
        return GENERIC_CLIENT_ERROR;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean isDataIntegrityFailure(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof DataIntegrityViolationException) {
                return true;
            }
            if (current instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    private static ErrorResponse findWrappedErrorResponse(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof ErrorResponse errorResponse) {
                return errorResponse;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return null;
    }

    private static String safeMessage(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        String trimmed = candidate.trim();
        if (trimmed.contains("\n") || trimmed.contains("\r")) {
            return fallback;
        }
        return trimmed;
    }

    /**
     * Request-supplied text is reflected into a single-line message, so it is
     * length-capped and stripped of anything that could break out of the message.
     */
    private static String safeSegment(String candidate) {
        String trimmed = safeMessage(candidate, "");
        return trimmed.length() > 60 ? trimmed.substring(0, 60) : trimmed;
    }

    private static ResponseEntity<Map<String, Object>> build(
            HttpStatusCode status,
            String message) {

        return build(status, message, Map.of());
    }

    private static ResponseEntity<Map<String, Object>> build(
            HttpStatusCode status,
            String message,
            Map<String, String> errors) {

        return ResponseEntity.status(status).body(envelope(status, message, errors));
    }

    private static Map<String, Object> envelope(
            HttpStatusCode status,
            String message,
            Map<String, String> errors) {

        String reason = status instanceof HttpStatus httpStatus
                ? httpStatus.getReasonPhrase()
                : status.toString();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", reason);
        body.put("message", message);
        body.put("errors", errors);

        return body;
    }
}
