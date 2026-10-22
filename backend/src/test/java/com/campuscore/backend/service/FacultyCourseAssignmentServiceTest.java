package com.campuscore.backend.service;

import com.campuscore.backend.service.AuditLogService;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.FacultyCourseAssignment;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Faculty-to-course assignment behaviour.
 *
 * <p>The overriding rule is that academic history is never touched: only the
 * assignment table is written, and previous assignments are closed rather than
 * deleted so the teaching history of a course survives a reassignment.
 */
class FacultyCourseAssignmentServiceTest {

    private FacultyCourseAssignmentRepository assignments;
    private CourseRepository courses;
    private FacultyRepository faculty;
    private EnrollmentRepository enrollments;

    private FacultyCourseAssignmentService service;

    private Course course;
    private Faculty jamie;

    @BeforeEach
    void setUp() {
        assignments = mock(FacultyCourseAssignmentRepository.class);
        courses = mock(CourseRepository.class);
        faculty = mock(FacultyRepository.class);
        enrollments = mock(EnrollmentRepository.class);

        when(assignments.save(any(FacultyCourseAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assignments.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new FacultyCourseAssignmentService(assignments, courses, faculty, enrollments, mock(AuditLogService.class));

        course = new Course();
        course.setCourseId(11L);
        course.setCourseCode("CSE101");
        course.setCourseName("Data Structures");
        course.setStatus(Course.Status.ACTIVE);

        jamie = new Faculty();
        jamie.setFacultyId(21L);
        jamie.setEmployeeNumber("FAC-001");
        jamie.setFirstName("Jamie");
        jamie.setLastName("Patel");
        jamie.setStatus(Faculty.Status.ACTIVE);

        when(courses.findById(11L)).thenReturn(Optional.of(course));
        when(faculty.findById(21L)).thenReturn(Optional.of(jamie));
    }

    private Faculty secondFaculty(long id, String employeeNumber) {
        Faculty other = new Faculty();
        other.setFacultyId(id);
        other.setEmployeeNumber(employeeNumber);
        other.setFirstName("Morgan");
        other.setLastName("Rivera");
        other.setStatus(Faculty.Status.ACTIVE);
        return other;
    }

    // ------------------------------------------------------------------
    // Assign
    // ------------------------------------------------------------------

    @Test
    void assignsAnActiveFacultyMemberToACourse() {
        var response = service.assignFaculty(11L, 21L);

        assertEquals(11L, response.getCourseId());
        assertEquals("CSE101", response.getCourseCode());
        assertEquals(21L, response.getFacultyId());
        assertEquals("FAC-001", response.getEmployeeNumber());
        assertEquals("Jamie Patel", response.getFacultyName());
        assertTrue(response.getActive());
        assertNull(response.getEndedAt());
    }

    @Test
    void aFacultyMemberCanTeachMultipleCourses() {
        Course other = new Course();
        other.setCourseId(12L);
        other.setCourseCode("CSE201");
        other.setCourseName("Database Management Systems");
        when(courses.findById(12L)).thenReturn(Optional.of(other));

        service.assignFaculty(11L, 21L);
        service.assignFaculty(12L, 21L);

        verify(assignments, times(2)).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void rejectsAssigningTheSameFacultyTwiceToTheSameCourse() {
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.of(existing(21L)));

        assertThrows(
                ResourceConflictException.class,
                () -> service.assignFaculty(11L, 21L));

        verify(assignments, never()).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void theSameFacultyMayReturnToACourseAfterTheAssignmentWasClosed() {
        FacultyCourseAssignment closed = existing(21L);
        closed.setEndedAt(java.time.LocalDateTime.now().minusDays(30));

        // No open assignment exists, so a fresh one is allowed. This is why the
        // schema carries no UNIQUE(course_id, faculty_id).
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.empty());

        var response = service.assignFaculty(11L, 21L);

        assertTrue(response.getActive());
        assertNotNull(closed.getEndedAt());
    }

    @Test
    void rejectsUnknownCourse() {
        when(courses.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assignFaculty(999L, 21L));
        verify(assignments, never()).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void rejectsUnknownFaculty() {
        when(faculty.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assignFaculty(11L, 999L));
        verify(assignments, never()).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void rejectsAssigningAnInactiveFacultyMember() {
        jamie.setStatus(Faculty.Status.INACTIVE);

        assertThrows(ResourceConflictException.class, () -> service.assignFaculty(11L, 21L));
        verify(assignments, never()).save(any(FacultyCourseAssignment.class));
    }

    // ------------------------------------------------------------------
    // Reassign
    // ------------------------------------------------------------------

    @Test
    void reassignmentClosesThePreviousAssignmentAndKeepsIt() {
        FacultyCourseAssignment previous = existing(21L);
        previous.setAssignmentId(500L);
        Faculty morgan = secondFaculty(22L, "FAC-002");
        when(faculty.findById(22L)).thenReturn(Optional.of(morgan));
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(11L))
                .thenReturn(new ArrayList<>(List.of(previous)));
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 22L))
                .thenReturn(Optional.empty());

        var response = service.reassignFaculty(11L, 22L);

        // previous row is closed, never deleted
        assertNotNull(previous.getEndedAt());
        verify(assignments).saveAll(List.of(previous));
        verify(assignments, never()).delete(any(FacultyCourseAssignment.class));

        // a new open assignment now owns the course
        assertEquals(22L, response.getFacultyId());
        assertTrue(response.getActive());
        assertNull(response.getEndedAt());
    }

    @Test
    void reassignmentDoesNotTouchAcademicRecords() {
        FacultyCourseAssignment previous = existing(21L);
        Faculty morgan = secondFaculty(22L, "FAC-002");
        when(faculty.findById(22L)).thenReturn(Optional.of(morgan));
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(11L))
                .thenReturn(new ArrayList<>(List.of(previous)));
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 22L))
                .thenReturn(Optional.empty());

        service.reassignFaculty(11L, 22L);

        // Enrolments are read only for the removal safety check, never written.
        verify(enrollments, never()).countByCourseCourseId(any());
    }

    @Test
    void reassignmentToTheAlreadyAssignedFacultyIsRejected() {
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(11L))
                .thenReturn(new ArrayList<>(List.of(existing(21L))));

        assertThrows(ResourceConflictException.class, () -> service.reassignFaculty(11L, 21L));
        verify(assignments, never()).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void reassignmentOfACourseWithNoCurrentOwnerJustCreatesTheAssignment() {
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(11L))
                .thenReturn(new ArrayList<>());
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.empty());

        var response = service.reassignFaculty(11L, 21L);

        assertTrue(response.getActive());
        verify(assignments, never()).saveAll(anyList());
    }

    // ------------------------------------------------------------------
    // Remove
    // ------------------------------------------------------------------

    @Test
    void removingAnAssignmentClosesItInsteadOfDeletingHistory() {
        FacultyCourseAssignment open = existing(21L);
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.of(open));
        when(assignments.countByCourseCourseIdAndEndedAtIsNull(11L)).thenReturn(2L);
        when(enrollments.countByCourseCourseId(11L)).thenReturn(5L);

        service.removeAssignment(11L, 21L);

        assertNotNull(open.getEndedAt());
        verify(assignments).save(open);
        verify(assignments, never()).delete(any(FacultyCourseAssignment.class));
    }

    @Test
    void removingTheLastAssignmentOfACourseWithEnrolmentsIsRefused() {
        FacultyCourseAssignment open = existing(21L);
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.of(open));
        when(assignments.countByCourseCourseIdAndEndedAtIsNull(11L)).thenReturn(1L);
        when(enrollments.countByCourseCourseId(11L)).thenReturn(5L);

        assertThrows(ResourceConflictException.class, () -> service.removeAssignment(11L, 21L));

        assertNull(open.getEndedAt());
        verify(assignments, never()).save(any(FacultyCourseAssignment.class));
    }

    @Test
    void removingTheLastAssignmentIsAllowedWhenTheCourseHasNoEnrolments() {
        FacultyCourseAssignment open = existing(21L);
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.of(open));
        when(assignments.countByCourseCourseIdAndEndedAtIsNull(11L)).thenReturn(1L);
        when(enrollments.countByCourseCourseId(11L)).thenReturn(0L);

        service.removeAssignment(11L, 21L);

        assertNotNull(open.getEndedAt());
    }

    @Test
    void removalFailsClosedWhenEnrolmentsCannotBeVerified() {
        FacultyCourseAssignment open = existing(21L);
        FacultyCourseAssignmentService reduced =
                new FacultyCourseAssignmentService(assignments, courses, faculty, null, mock(AuditLogService.class));

        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.of(open));
        when(assignments.countByCourseCourseIdAndEndedAtIsNull(11L)).thenReturn(1L);

        assertThrows(ResourceConflictException.class, () -> reduced.removeAssignment(11L, 21L));
    }

    @Test
    void removingAnAssignmentThatDoesNotExistIsNotFound() {
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(11L, 21L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.removeAssignment(11L, 21L));
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    @Test
    void listsOnlyCurrentAssignmentsForACourse() {
        FacultyCourseAssignment open = existing(21L);
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(11L))
                .thenReturn(List.of(open));

        var active = service.getActiveAssignments(11L);

        assertEquals(1, active.size());
        assertTrue(active.get(0).getActive());
    }

    @Test
    void historyIncludesClosedAssignments() {
        FacultyCourseAssignment open = existing(21L);
        FacultyCourseAssignment closed = existing(21L);
        closed.setEndedAt(java.time.LocalDateTime.now().minusMonths(6));
        when(assignments.findByCourseCourseIdOrderByAssignedAtDesc(11L))
                .thenReturn(List.of(open, closed));

        var history = service.getAssignmentHistory(11L);

        assertEquals(2, history.size());
        assertTrue(history.get(0).getActive());
        assertFalse(history.get(1).getActive());
    }

    @Test
    void listsCoursesForAFacultyMember() {
        FacultyCourseAssignment open = existing(21L);
        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(21L))
                .thenReturn(List.of(open));

        var assigned = service.getActiveAssignmentsForFaculty(21L);

        assertEquals(1, assigned.size());
        assertEquals("CSE101", assigned.get(0).getCourseCode());
    }

    @Test
    void listingCoursesForAnUnknownFacultyIsNotFound() {
        when(faculty.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getActiveAssignmentsForFaculty(404L));
    }

    @Test
    void readingAssignmentsNeverWrites() {
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(11L))
                .thenReturn(List.of(existing(21L)));

        service.getActiveAssignments(11L);

        verifyNoInteractions(enrollments);
    }

    private FacultyCourseAssignment existing(long facultyId) {
        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setAssignmentId(facultyId);
        assignment.setCourse(course);
        assignment.setFaculty(jamie);
        assignment.setAssignedAt(java.time.LocalDateTime.now().minusYears(1));
        return assignment;
    }
}
