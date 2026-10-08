package com.campuscore.backend.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import com.campuscore.backend.entity.Course;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ------------------------------------------------------------------
    // Envelope shape
    // ------------------------------------------------------------------

    @Test
    void everyResponseCarriesTheSameFourKeys() {
        Map<String, Object> body = body(handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Course not found with ID: 5")));

        assertEquals(Map.of("status", 404, "error", "Not Found",
                        "message", "Course not found with ID: 5",
                        "errors", Map.of()),
                body);
    }

    // ------------------------------------------------------------------
    // Validation -> 400
    // ------------------------------------------------------------------

    @Test
    void beanValidationFailuresBecomeBadRequestWithFieldErrors() {
        ResponseEntity<Object> response =
                handler.handleMethodArgumentNotValid(
                        validationFailure("credits", "Credits must be positive"),
                        new org.springframework.http.HttpHeaders(),
                        HttpStatus.BAD_REQUEST,
                        webRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400, body.get("status"));
        assertEquals("Validation failed", body.get("message"));
        assertEquals(Map.of("credits", "Credits must be positive"), body.get("errors"));
    }

    @Test
    void beanConstraintViolationsBecomeBadRequest() {
        ConstraintViolationException exception = new ConstraintViolationException(
                "size violated", java.util.Set.of());

        ResponseEntity<Map<String, Object>> response =
                handler.handleConstraintViolationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().get("errors"));
    }

    @Test
    void malformedRequestBodyBecomesBadRequestWithoutLeakingParserInternals() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` "
                        + "from String \"not-a-date\"\n at [Source: (String)\"not-a-date\"; line: 1]",
                (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                exception,
                null,
                new org.springframework.http.HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                webRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Request body is missing or malformed.", body.get("message"));
        assertFalse(body.toString().contains("not-a-date"));
        assertFalse(body.toString().contains("org.springframework"));
    }

    @Test
    void missingRequestParameterBecomesBadRequestNamingOnlyTheParameter() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("size", "Integer");

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                exception,
                null,
                new org.springframework.http.HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                webRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Required request parameter 'size' is missing or invalid.",
                body.get("message"));
    }

    @Test
    void illegalArgumentBecomesBadRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgumentException(
                        new IllegalArgumentException("User role must be STUDENT to link a student profile"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ------------------------------------------------------------------
    // Dependency / FK conflicts -> 409
    // ------------------------------------------------------------------

    @Test
    void foreignKeyAndUniqueViolationsBecomeConflictWithoutLeakingSql() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement [delete from course where course_id=?] "
                        + "[ConstraintViolationException: "
                        + "fk_enrollment_course foreign key constraint fails]");

        ResponseEntity<Map<String, Object>> response =
                handler.handleDataIntegrityViolationException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));

        String rendered = response.getBody().toString();
        assertFalse(rendered.contains("course_id"));
        assertFalse(rendered.contains("fk_enrollment_course"));
        assertFalse(rendered.contains("delete from"));
    }

    @Test
    void dependencyConflictBecomesConflict() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResourceConflictException(new ResourceConflictException(
                        "Course has 3 enrollment(s). Deactivate or archive it instead of deleting."));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Course has 3 enrollment(s). Deactivate or archive it instead of deleting.",
                response.getBody().get("message"));
    }

    @Test
    void optimisticLockFailureBecomesConflict() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleOptimisticLockException(new OptimisticLockException()).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleOptimisticLockException(
                        new ObjectOptimisticLockingFailureException(Course.class, 1L)).getStatusCode());
    }

    @Test
    void pessimisticLockFailureBecomesConflict() {
        assertEquals(HttpStatus.CONFLICT,
                handler.handleOptimisticLockException(
                        new PessimisticLockingFailureException("row locked")).getStatusCode());
    }

    @Test
    void integrityFailureRaisedDuringCommitStillBecomesConflict() {
        TransactionSystemException exception = new TransactionSystemException(
                "commit failed",
                new DataIntegrityViolationException("fk_faculty_department violated"));

        assertEquals(HttpStatus.CONFLICT,
                handler.handleTransactionException(exception).getStatusCode());
    }

    @Test
    void unrelatedTransactionFailureBecomesServerErrorWithoutDetails() {
        TransactionSystemException exception = new TransactionSystemException(
                "connection reset by peer");

        ResponseEntity<Map<String, Object>> response = handler.handleTransactionException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().toString().contains("connection reset"));
    }

    // ------------------------------------------------------------------
    // Unexpected failures -> safe 500
    // ------------------------------------------------------------------

    @Test
    void unexpectedExceptionBecomesServerErrorWithNoInternalDetail() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpectedException(new IllegalStateException(
                        "java.lang.NullPointerException: cannot invoke "
                                + "\"com.campuscore.backend.entity.Course.getDepartment()\" because \"course\" is null"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        String rendered = response.getBody().toString();
        assertFalse(rendered.contains("NullPointerException"));
        assertFalse(rendered.contains("com.campuscore"));
        assertFalse(rendered.contains("java.lang"));
        assertFalse(rendered.contains("at com.campuscore"));
    }

    @Test
    void serverSideErrorResponseIsStillGeneric() {
        ResponseEntity<Object> response = handler.handleExceptionInternal(
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "jdbc:h2:mem:campuscore pool exhausted"),
                null,
                new org.springframework.http.HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                webRequest());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(GlobalExceptionHandler.GENERIC_SERVER_ERROR, body.get("message"));
        assertFalse(body.toString().contains("jdbc:"));
    }

    @Test
    void wrappedClientErrorKeepsItsStatusInsteadOfBecomingAServerFault() {
        RuntimeException wrapper = new IllegalStateException(
                "service layer rewrapped the failure",
                new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "size must be between 1 and 100"));

        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpectedException(wrapper);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("size must be between 1 and 100", response.getBody().get("message"));
    }

    @Test
    void wrappedServerErrorStillBecomesAGeneric500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpectedException(new IllegalStateException(
                        "rethrow",
                        new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "jdbc:h2:mem:campuscore pool exhausted")));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(GlobalExceptionHandler.GENERIC_SERVER_ERROR,
                response.getBody().get("message"));
        assertFalse(response.getBody().toString().contains("jdbc:"));
    }

    // ------------------------------------------------------------------
    // Framework statuses are preserved
    // ------------------------------------------------------------------

    @Test
    void unsupportedHttpMethodKeepsIts405Status() {
        ResponseEntity<Object> response = handler.handleExceptionInternal(
                new HttpRequestMethodNotSupportedException("DELETE"),
                null,
                new org.springframework.http.HttpHeaders(),
                HttpStatus.METHOD_NOT_ALLOWED,
                webRequest());

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void deliberateResponseStatusIsPreserved() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResponseStatusException(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Provide page >= 0 and size between 1 and 100"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Provide page >= 0 and size between 1 and 100",
                response.getBody().get("message"));
    }

    @Test
    void blankMessagesFallBackToSafeDefaults() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResourceNotFoundException(new ResourceNotFoundException("   "));

        assertEquals(GlobalExceptionHandler.GENERIC_CLIENT_ERROR,
                response.getBody().get("message"));
    }

    @Test
    void multiLineMessagesAreNotEchoed() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleResourceConflictException(new ResourceConflictException(
                        "first line\n\tat com.campuscore.backend.service.CourseService.deleteCourse(CourseService.java:116)"));

        assertEquals(GlobalExceptionHandler.DEPENDENCY_CONFLICT,
                response.getBody().get("message"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static MethodArgumentNotValidException validationFailure(
            String field,
            String message) {

        Course target = new Course();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "courseRequest");
        bindingResult.addError(new FieldError("courseRequest", field, message));

        return new MethodArgumentNotValidException(
                (org.springframework.core.MethodParameter) null, bindingResult);
    }

    private static ServletWebRequest webRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses");
        return new ServletWebRequest(request, new org.springframework.mock.web.MockHttpServletResponse());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<Map<String, Object>> response) {
        assertNotNull(response.getBody());
        return response.getBody();
    }
}
