package com.campuscore.backend.service;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The assignment relationship introduces new dependents, so course and faculty
 * deletion must stay dependency-aware now that it exists.
 */
class AssignmentDependencyLifecycleTest {

    private Course course(long id, String code) {
        Course course = new Course();
        course.setCourseId(id);
        course.setCourseCode(code);
        course.setCourseName("Course " + code);
        return course;
    }

    private Faculty faculty(long id, String employeeNumber) {
        Faculty faculty = new Faculty();
        faculty.setFacultyId(id);
        faculty.setEmployeeNumber(employeeNumber);
        return faculty;
    }

    // ------------------------------------------------------------------
    // Course
    // ------------------------------------------------------------------

    @Test
    void courseWithAnAssignmentCannotBeDeleted() {
        CourseRepository courses = mock(CourseRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        Course target = course(11L, "CSE101");
        when(courses.findById(11L)).thenReturn(Optional.of(target));
        when(assignments.countByCourseCourseId(11L)).thenReturn(1L);

        CourseService service = new CourseService(courses,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        assertThrows(ResourceConflictException.class, () -> service.deleteCourse(11L));
        verify(courses, never()).delete(target);
    }

    @Test
    void courseDependencySummaryReportsAssignmentCount() {
        CourseRepository courses = mock(CourseRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        when(courses.findById(11L)).thenReturn(Optional.of(course(11L, "CSE101")));
        when(assignments.countByCourseCourseId(11L)).thenReturn(2L);

        CourseService service = new CourseService(courses,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        var summary = service.getCourseDependencies(11L);

        assertEquals(2L, summary.getDependencyCounts().get("facultyAssignments"));
        assertFalse(summary.isCanDelete());
    }

    @Test
    void courseWithNoAssignmentsIsStillDeletable() {
        CourseRepository courses = mock(CourseRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        Course target = course(11L, "CSE101");
        when(courses.findById(11L)).thenReturn(Optional.of(target));

        CourseService service = new CourseService(courses,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        service.deleteCourse(11L);

        verify(courses).delete(target);
    }

    @Test
    void courseDeleteIsRefusedWhenAssignmentsCannotBeVerified() {
        CourseRepository courses = mock(CourseRepository.class);

        Course target = course(11L, "CSE101");
        when(courses.findById(11L)).thenReturn(Optional.of(target));

        // Reduced constructor: the assignment repository is absent and must not be
        // mistaken for "this course has no assignments".
        CourseService service = new CourseService(courses);

        assertThrows(ResourceConflictException.class, () -> service.deleteCourse(11L));
        verify(courses, never()).delete(target);
    }

    @Test
    void deactivatingACourseIsUnaffectedByAssignments() {
        CourseRepository courses = mock(CourseRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        Course target = course(11L, "CSE101");
        target.setStatus(Course.Status.ACTIVE);
        when(courses.findById(11L)).thenReturn(Optional.of(target));
        when(courses.save(org.mockito.ArgumentMatchers.any(Course.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assignments.countByCourseCourseId(11L)).thenReturn(1L);

        CourseService service = new CourseService(courses,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        assertEquals(Course.Status.INACTIVE, service.deactivateCourse(11L).getStatus());
    }

    // ------------------------------------------------------------------
    // Faculty
    // ------------------------------------------------------------------

    @Test
    void facultyWithAnAssignmentCannotBeDeleted() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        Faculty target = faculty(21L, "FAC-001");
        when(facultyRepository.findById(21L)).thenReturn(Optional.of(target));
        when(assignments.countByFacultyFacultyId(21L)).thenReturn(3L);

        FacultyService service = new FacultyService(facultyRepository,
                mock(UserRepository.class),
                mock(com.campuscore.backend.repository.DepartmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        assertThrows(ResourceConflictException.class, () -> service.deleteFaculty(21L));
        verify(facultyRepository, never()).delete(target);
    }

    @Test
    void facultyDependencySummaryReportsAssignmentCount() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        when(facultyRepository.findById(21L)).thenReturn(Optional.of(faculty(21L, "FAC-001")));
        when(assignments.countByFacultyFacultyId(21L)).thenReturn(3L);

        FacultyService service = new FacultyService(facultyRepository,
                mock(UserRepository.class),
                mock(com.campuscore.backend.repository.DepartmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        var summary = service.getFacultyDependencies(21L);

        assertEquals(3L, summary.getDependencyCounts().get("courseAssignments"));
        assertFalse(summary.isCanDelete());
        assertTrue(summary.isCanArchive());
    }

    @Test
    void facultyWithNoAssignmentsIsStillDeletable() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        Faculty target = faculty(21L, "FAC-001");
        when(facultyRepository.findById(21L)).thenReturn(Optional.of(target));

        FacultyService service = new FacultyService(facultyRepository,
                mock(UserRepository.class),
                mock(com.campuscore.backend.repository.DepartmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        service.deleteFaculty(21L);

        verify(facultyRepository).delete(target);
    }

    @Test
    void facultyDeleteIsRefusedWhenAssignmentsCannotBeVerified() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);

        Faculty target = faculty(21L, "FAC-001");
        when(facultyRepository.findById(21L)).thenReturn(Optional.of(target));

        FacultyService service = new FacultyService(facultyRepository,
                mock(UserRepository.class),
                mock(com.campuscore.backend.repository.DepartmentRepository.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteFaculty(21L));
        verify(facultyRepository, never()).delete(target);
    }

    @Test
    void closedAssignmentsStillBlockFacultyDeletion() {
        // A reassignment closes the old row rather than deleting it, so history
        // keeps blocking deletion. That is intentional: the teaching record is
        // preserved and deactivation is offered instead.
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        FacultyCourseAssignmentRepository assignments = mock(FacultyCourseAssignmentRepository.class);

        Faculty target = faculty(21L, "FAC-001");
        when(facultyRepository.findById(21L)).thenReturn(Optional.of(target));
        when(assignments.countByFacultyFacultyId(21L)).thenReturn(2L);
        when(assignments.countByFacultyFacultyIdAndEndedAtIsNull(21L)).thenReturn(0L);

        FacultyService service = new FacultyService(facultyRepository,
                mock(UserRepository.class),
                mock(com.campuscore.backend.repository.DepartmentRepository.class),
                mock(AttendanceSessionRepository.class),
                assignments);

        assertThrows(ResourceConflictException.class, () -> service.deleteFaculty(21L));
    }
}
