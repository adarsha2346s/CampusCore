package com.campuscore.backend.service;

import com.campuscore.backend.service.AuditLogService;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.MarkRepository;
import com.campuscore.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Enrollment lifecycle.
 *
 * <p>Before this slice the status was only ever ENROLLED, so DROPPED and
 * COMPLETED were unreachable. These tests pin the update and delete rules, and in
 * particular the integrity guards that stop academic history being silently
 * re-termed or orphaned.
 */
class EnrollmentLifecycleTest {

    private static final Long ENROLLMENT_ID = 3000L;
    private static final Long STUDENT_ID = 7L;
    private static final Long COURSE_ID = 11L;

    private EnrollmentRepository enrollments;
    private MarkRepository marks;
    private AttendanceRecordRepository records;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentRepository.class);
        marks = mock(MarkRepository.class);
        records = mock(AttendanceRecordRepository.class);

        service = new EnrollmentService(
                enrollments,
                mock(StudentRepository.class),
                mock(CourseRepository.class),
                mock(AccessScope.class),
                marks,
                records, mock(AuditLogService.class));
    }

    private Enrollment existing() {
        Student student = new Student();
        student.setStudentId(STUDENT_ID);

        Course course = new Course();
        course.setCourseId(COURSE_ID);

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(ENROLLMENT_ID);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setSemester("Fall");
        enrollment.setAcademicYear("2026-2027");
        enrollment.setStatus(Enrollment.Status.ENROLLED);
        return enrollment;
    }

    private void withEnrollment() {
        when(enrollments.findById(ENROLLMENT_ID)).thenReturn(Optional.of(existing()));
        when(enrollments.save(any(Enrollment.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void noAcademicRecords() {
        when(marks.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);
        when(records.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);
    }

    // ------------------------------------------------------------------
    // Status transitions become reachable
    // ------------------------------------------------------------------

    @Test
    void anEnrollmentCanBeMarkedDropped() {
        withEnrollment();

        Enrollment updated = service.updateEnrollment(
                ENROLLMENT_ID, "Fall", "2026-2027", Enrollment.Status.DROPPED);

        assertEquals(Enrollment.Status.DROPPED, updated.getStatus());
    }

    @Test
    void anEnrollmentCanBeMarkedCompleted() {
        withEnrollment();

        Enrollment updated = service.updateEnrollment(
                ENROLLMENT_ID, "Fall", "2026-2027", Enrollment.Status.COMPLETED);

        assertEquals(Enrollment.Status.COMPLETED, updated.getStatus());
    }

    @Test
    void statusCanChangeEvenWhenAcademicRecordsExist() {
        // Status is the archive control and must stay available, otherwise a
        // graded term could never be closed out.
        withEnrollment();
        when(marks.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(4L);
        when(records.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(3L);

        Enrollment updated = service.updateEnrollment(
                ENROLLMENT_ID, "Fall", "2026-2027", Enrollment.Status.COMPLETED);

        assertEquals(Enrollment.Status.COMPLETED, updated.getStatus());
    }

    @Test
    void termCanBeCorrectedWhileNoAcademicRecordsExist() {
        withEnrollment();
        noAcademicRecords();

        Enrollment updated = service.updateEnrollment(
                ENROLLMENT_ID, "Spring", "2027-2028", Enrollment.Status.ENROLLED);

        assertEquals("Spring", updated.getSemester());
        assertEquals("2027-2028", updated.getAcademicYear());
    }

    // ------------------------------------------------------------------
    // Integrity guards
    // ------------------------------------------------------------------

    @Test
    void termCannotBeChangedOnceMarksExist() {
        withEnrollment();
        when(marks.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(2L);
        when(records.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);

        assertThrows(ResourceConflictException.class, () -> service.updateEnrollment(
                ENROLLMENT_ID, "Spring", "2027-2028", Enrollment.Status.ENROLLED));

        verify(enrollments, never()).save(any(Enrollment.class));
    }

    @Test
    void termCannotBeChangedOnceAttendanceExists() {
        withEnrollment();
        when(marks.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);
        when(records.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(5L);

        assertThrows(ResourceConflictException.class, () -> service.updateEnrollment(
                ENROLLMENT_ID, "Spring", "2027-2028", Enrollment.Status.ENROLLED));
    }

    @Test
    void termChangeIsAllowedWhenOnlyTheYearMovesAndNothingIsGraded() {
        withEnrollment();
        noAcademicRecords();

        assertEquals("2027-2028", service.updateEnrollment(
                ENROLLMENT_ID, "Fall", "2027-2028", Enrollment.Status.ENROLLED)
                .getAcademicYear());
    }

    @Test
    void termChangeCannotCollideWithAnotherEnrollment() {
        withEnrollment();
        noAcademicRecords();
        when(enrollments
                .existsByStudentStudentIdAndCourseCourseIdAndSemesterAndAcademicYear(
                        STUDENT_ID, COURSE_ID, "Spring", "2027-2028"))
                .thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.updateEnrollment(
                ENROLLMENT_ID, "Spring", "2027-2028", Enrollment.Status.ENROLLED));
    }

    @Test
    void updatingAnUnknownEnrollmentIsNotFound() {
        when(enrollments.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateEnrollment(
                404L, "Fall", "2026-2027", Enrollment.Status.ENROLLED));
    }

    // ------------------------------------------------------------------
    // Dependency-aware delete
    // ------------------------------------------------------------------

    @Test
    void anEnrollmentWithNoAcademicRecordsCanBeDeleted() {
        withEnrollment();
        noAcademicRecords();

        service.deleteEnrollment(ENROLLMENT_ID);

        verify(enrollments).delete(any(Enrollment.class));
    }

    @Test
    void anEnrollmentWithMarksCannotBeDeleted() {
        withEnrollment();
        when(marks.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(3L);
        when(records.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);

        ResourceConflictException failure = assertThrows(
                ResourceConflictException.class, () -> service.deleteEnrollment(ENROLLMENT_ID));

        org.junit.jupiter.api.Assertions.assertTrue(
                failure.getMessage().contains("Change the status instead"), failure.getMessage());
        verify(enrollments, never()).delete(any(Enrollment.class));
    }

    @Test
    void anEnrollmentWithAttendanceCannotBeDeleted() {
        withEnrollment();
        when(marks.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);
        when(records.countByEnrollmentEnrollmentId(ENROLLMENT_ID)).thenReturn(2L);

        assertThrows(ResourceConflictException.class, () -> service.deleteEnrollment(ENROLLMENT_ID));
        verify(enrollments, never()).delete(any(Enrollment.class));
    }

    @Test
    void deletingAnUnknownEnrollmentIsNotFound() {
        when(enrollments.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteEnrollment(404L));
    }

    // ------------------------------------------------------------------
    // Fail closed
    // ------------------------------------------------------------------

    @Test
    void deleteFailsClosedWhenDependentsCannotBeVerified() {
        EnrollmentService unscoped = new EnrollmentService(
                enrollments,
                mock(StudentRepository.class),
                mock(CourseRepository.class), mock(AuditLogService.class));
        when(enrollments.findById(ENROLLMENT_ID)).thenReturn(Optional.of(existing()));

        assertThrows(ResourceConflictException.class, () -> unscoped.deleteEnrollment(ENROLLMENT_ID));
        verify(enrollments, never()).delete(any(Enrollment.class));
    }

    @Test
    void termChangeFailsClosedWhenDependentsCannotBeVerified() {
        EnrollmentService unscoped = new EnrollmentService(
                enrollments,
                mock(StudentRepository.class),
                mock(CourseRepository.class), mock(AuditLogService.class));
        when(enrollments.findById(ENROLLMENT_ID)).thenReturn(Optional.of(existing()));

        assertThrows(ResourceConflictException.class, () -> unscoped.updateEnrollment(
                ENROLLMENT_ID, "Spring", "2027-2028", Enrollment.Status.ENROLLED));
    }

}
