package com.campuscore.backend.service;

import com.campuscore.backend.dto.MarkRequest;
import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.MarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mark update and delete.
 *
 * <p>A mark is a leaf record, so deletion has no dependents to protect and the
 * interesting rules are the ones on correction: the value must stay inside the
 * assessment's ceiling, the enrollment and assessment must not move, and the
 * original entry timestamp must survive a correction.
 */
class MarkLifecycleTest {

    private static final Long MARK_ID = 700L;
    private static final Long ENROLLMENT_ID = 300L;
    private static final Long ASSESSMENT_ID = 42L;
    private static final LocalDateTime ENTERED_AT = LocalDateTime.of(2026, 3, 14, 9, 30);

    private MarkRepository marks;
    private AccessScope accessScope;
    private MarkService service;

    @BeforeEach
    void setUp() {
        marks = mock(MarkRepository.class);
        accessScope = mock(AccessScope.class);

        service = new MarkService(
                marks,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                accessScope);
    }

    private Mark existing(String maxMarks) {
        Course course = new Course();
        course.setCourseId(7L);

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(ASSESSMENT_ID);
        assessment.setCourse(course);
        assessment.setName("Midterm Exam");
        assessment.setMaxMarks(new BigDecimal(maxMarks));

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(ENROLLMENT_ID);

        Mark mark = new Mark();
        mark.setMarkId(MARK_ID);
        mark.setEnrollment(enrollment);
        mark.setAssessment(assessment);
        mark.setMarksObtained(new BigDecimal("40.00"));
        mark.setEnteredAt(ENTERED_AT);
        return mark;
    }

    private void savingSucceeds() {
        when(marks.save(any(Mark.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Update applies the corrected value")
    void updateAppliesCorrectedValue() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        Mark updated = service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("87.50")));

        assertEquals(0, updated.getMarksObtained().compareTo(new BigDecimal("87.50")));
        verify(accessScope).requireEnrollmentAccess(ENROLLMENT_ID);
    }

    @Test
    @DisplayName("Update keeps the enrollment and assessment it was recorded against")
    void updateKeepsEnrollmentAndAssessment() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        Mark updated = service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("55.00")));

        assertEquals(ENROLLMENT_ID, updated.getEnrollment().getEnrollmentId());
        assertEquals(ASSESSMENT_ID, updated.getAssessment().getAssessmentId());
    }

    @Test
    @DisplayName("Update preserves the original entry timestamp")
    void updatePreservesEnteredAt() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        Mark updated = service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("12.00")));

        assertEquals(ENTERED_AT, updated.getEnteredAt());
    }

    @Test
    @DisplayName("Update rejects a value above the assessment maximum")
    void updateRejectsValueAboveMaximum() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class,
                () -> service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("120.00"))));

        assertTrue(failure.getMessage().contains("greater than maximum marks"));
        assertEquals(0, stored.getMarksObtained().compareTo(new BigDecimal("40.00")));
        verify(marks, never()).save(any(Mark.class));
    }

    @Test
    @DisplayName("Update accepts a value exactly equal to the maximum")
    void updateAcceptsValueEqualToMaximum() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        Mark updated = service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("100.00")));

        assertEquals(0, updated.getMarksObtained().compareTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Update rejects a negative value")
    void updateRejectsNegativeValue() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class,
                () -> service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("-5.00"))));

        assertTrue(failure.getMessage().contains("cannot be negative"));
        verify(marks, never()).save(any(Mark.class));
    }

    @Test
    @DisplayName("Update fails closed when the maximum has since been lowered below the value")
    void updateFailsWhenCeilingLoweredUnderValue() {
        // The maximum is editable until the first mark exists, so a stored value
        // can end up above it if the ceiling is later corrected by other means.
        Mark stored = existing("30.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));

        assertThrows(ResourceConflictException.class,
                () -> service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("35.00"))));

        verify(marks, never()).save(any(Mark.class));
    }

    @Test
    @DisplayName("Update reports not found for an unknown mark")
    void updateUnknownMark() {
        when(marks.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateMark(404L, new MarkRequest(new BigDecimal("10.00"))));
    }

    @Test
    @DisplayName("Update refuses a caller outside the enrollment's scope")
    void updateRefusesOutOfScopeCaller() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));
        org.mockito.Mockito.doThrow(new AccessDeniedException("not assigned"))
                .when(accessScope).requireEnrollmentAccess(ENROLLMENT_ID);

        assertThrows(AccessDeniedException.class,
                () -> service.updateMark(MARK_ID, new MarkRequest(new BigDecimal("50.00"))));

        verify(marks, never()).save(any(Mark.class));
    }

    @Test
    @DisplayName("Delete removes the mark and re-checks enrollment scope")
    void deleteRemovesMark() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));

        service.deleteMark(MARK_ID);

        verify(marks).delete(stored);
        verify(accessScope).requireEnrollmentAccess(ENROLLMENT_ID);
    }

    @Test
    @DisplayName("Delete refuses a caller outside the enrollment's scope")
    void deleteRefusesOutOfScopeCaller() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));
        org.mockito.Mockito.doThrow(new AccessDeniedException("not assigned"))
                .when(accessScope).requireEnrollmentAccess(ENROLLMENT_ID);

        assertThrows(AccessDeniedException.class, () -> service.deleteMark(MARK_ID));

        verify(marks, never()).delete(any(Mark.class));
    }

    @Test
    @DisplayName("Delete reports not found for an unknown mark")
    void deleteUnknownMark() {
        when(marks.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteMark(404L));

        verify(marks, never()).delete(any(Mark.class));
    }

    @Test
    @DisplayName("Scope guards fail closed when the scope service is unwired")
    void scopeFailsClosedWithoutAccessScope() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));

        MarkService unwired = new MarkService(
                marks,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                null);

        assertThrows(AccessDeniedException.class,
                () -> unwired.updateMark(MARK_ID, new MarkRequest(new BigDecimal("50.00"))));
        assertThrows(AccessDeniedException.class, () -> unwired.deleteMark(MARK_ID));

        verify(marks, never()).save(any(Mark.class));
        verify(marks, never()).delete(any(Mark.class));
    }

    @Test
    @DisplayName("Existing read paths keep their scope checks")
    void readPathsStillScoped() {
        Mark stored = existing("100.00");
        when(marks.findById(MARK_ID)).thenReturn(Optional.of(stored));

        assertEquals(MARK_ID, service.getMarkById(MARK_ID).getMarkId());

        verify(accessScope).requireEnrollmentAccess(ENROLLMENT_ID);
    }
}