package com.campuscore.backend.service;

import com.campuscore.backend.dto.AssessmentRequest;
import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.MarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Assessment update and dependency-aware delete.
 *
 * <p>An assessment is the parent of mark rows, so two guards matter: rescaling
 * maximum marks once grades exist, and deleting a graded assessment. The course is
 * not editable, which keeps marks attached to the offering that owns them.
 */
class AssessmentLifecycleTest {

    private static final Long ASSESSMENT_ID = 42L;

    private AssessmentRepository assessments;
    private MarkRepository marks;
    private AssessmentService service;

    @BeforeEach
    void setUp() {
        assessments = mock(AssessmentRepository.class);
        marks = mock(MarkRepository.class);

        service = new AssessmentService(
                assessments,
                mock(CourseRepository.class),
                mock(AccessScope.class),
                marks);
    }

    private Assessment existing() {
        Department department = new Department();
        department.setDepartmentId(1L);

        Course course = new Course();
        course.setCourseId(7L);
        course.setDepartment(department);

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(ASSESSMENT_ID);
        assessment.setCourse(course);
        assessment.setName("Midterm Exam");
        assessment.setAssessmentType(Assessment.AssessmentType.MIDTERM);
        assessment.setMaxMarks(new BigDecimal("100.00"));
        assessment.setWeight(new BigDecimal("30.00"));
        return assessment;
    }

    private AssessmentRequest request(String name, String maxMarks) {
        return new AssessmentRequest(
                name,
                Assessment.AssessmentType.MIDTERM,
                new BigDecimal(maxMarks),
                new BigDecimal("30.00"),
                LocalDate.of(2026, 3, 12));
    }

    private void savingSucceeds() {
        when(assessments.save(any(Assessment.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Update applies name, type, weight and date")
    void updateAppliesEditableFields() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();

        AssessmentRequest request = new AssessmentRequest(
                "Final Exam",
                Assessment.AssessmentType.FINAL,
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                LocalDate.of(2026, 5, 4));

        Assessment updated = service.updateAssessment(ASSESSMENT_ID, request);

        assertEquals("Final Exam", updated.getName());
        assertEquals(Assessment.AssessmentType.FINAL, updated.getAssessmentType());
        assertEquals(0, updated.getMaxMarks().compareTo(new BigDecimal("100.00")));
        assertEquals(0, updated.getWeight().compareTo(new BigDecimal("40.00")));
        assertEquals(LocalDate.of(2026, 5, 4), updated.getAssessmentDate());
    }

    @Test
    @DisplayName("Update never reassigns the owning course")
    void updateKeepsCourseImmutable() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(0L);

        Assessment updated = service.updateAssessment(
                ASSESSMENT_ID, request("Midterm Exam", "50.00"));

        assertEquals(7L, updated.getCourse().getCourseId());
    }

    @Test
    @DisplayName("Maximum marks can be rescaled while no marks reference the assessment")
    void maxMarksRescalesBeforeAnyMarksExist() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(0L);

        Assessment updated = service.updateAssessment(
                ASSESSMENT_ID, request("Midterm Exam", "50.00"));

        assertEquals(0, updated.getMaxMarks().compareTo(new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("Maximum marks is locked once marks reference the assessment")
    void maxMarksLockedWhenMarksExist() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(3L);

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class,
                () -> service.updateAssessment(ASSESSMENT_ID, request("Midterm Exam", "50.00")));

        assertEquals(true, failure.getMessage().contains("Maximum marks"));
        verify(assessments, never()).save(any(Assessment.class));
    }

    @Test
    @DisplayName("Renaming stays allowed even when marks exist")
    void renameAllowedWhenMarksExist() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(3L);

        Assessment updated = service.updateAssessment(
                ASSESSMENT_ID, request("Midterm Exam (Rescheduled)", "100.00"));

        assertEquals("Midterm Exam (Rescheduled)", updated.getName());
        assertEquals(0, updated.getMaxMarks().compareTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Deleting an ungraded assessment succeeds")
    void deleteSucceedsWithoutMarks() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(0L);

        service.deleteAssessment(ASSESSMENT_ID);

        verify(assessments).delete(stored);
    }

    @Test
    @DisplayName("Deleting an assessment with marks is refused")
    void deleteBlockedWhenMarksExist() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(5L);

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class,
                () -> service.deleteAssessment(ASSESSMENT_ID));

        assertEquals(true, failure.getMessage().contains("marks reference it"));
        verify(assessments, never()).delete(any(Assessment.class));
    }

    @Test
    @DisplayName("Updating an unknown assessment reports not found")
    void updateUnknownAssessment() {
        when(assessments.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateAssessment(404L, request("Midterm Exam", "100.00")));
    }

    @Test
    @DisplayName("Deleting an unknown assessment reports not found")
    void deleteUnknownAssessment() {
        when(assessments.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteAssessment(404L));
    }

    @Test
    @DisplayName("Dependency guards fail closed when the mark repository is unavailable")
    void guardsFailClosedWithoutMarkRepository() {
        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));

        AssessmentService unwired = new AssessmentService(
                assessments, mock(CourseRepository.class), mock(AccessScope.class), null);

        ResourceConflictException updateFailure = assertThrows(
                ResourceConflictException.class,
                () -> unwired.updateAssessment(ASSESSMENT_ID, request("Midterm Exam", "50.00")));
        assertEquals(true, updateFailure.getMessage().contains("cannot be verified"));

        ResourceConflictException deleteFailure = assertThrows(
                ResourceConflictException.class,
                () -> unwired.deleteAssessment(ASSESSMENT_ID));
        assertEquals(true, deleteFailure.getMessage().contains("cannot be verified"));

        verify(assessments, never()).delete(any(Assessment.class));
        verify(assessments, never()).save(any(Assessment.class));
    }

    @Test
    @DisplayName("Course is resolved from the stored assessment, not the request")
    void courseLookupUntouchedOnUpdate() {
        CourseRepository courses = mock(CourseRepository.class);
        AssessmentService scoped = new AssessmentService(assessments, courses, mock(AccessScope.class), marks);

        Assessment stored = existing();
        when(assessments.findById(ASSESSMENT_ID)).thenReturn(Optional.of(stored));
        savingSucceeds();
        when(marks.countByAssessmentAssessmentId(ASSESSMENT_ID)).thenReturn(0L);

        Assessment updated = scoped.updateAssessment(
                ASSESSMENT_ID, request("Midterm Exam", "100.00"));

        assertSame(stored.getCourse(), updated.getCourse());
        verify(courses, never()).findById(any());
    }
}