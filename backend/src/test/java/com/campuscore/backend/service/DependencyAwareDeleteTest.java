package com.campuscore.backend.service;

import com.campuscore.backend.service.AuditLogService;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Deletion must never remove a record that other rows still point at, and must
 * never assume "no dependencies" just because a lookup was unavailable.
 */
class DependencyAwareDeleteTest {

    // ------------------------------------------------------------------
    // Refuses to delete while dependents exist
    // ------------------------------------------------------------------

    @Test
    void courseWithEnrollmentsIsRefusedWithConflict() {
        CourseRepository courses = mock(CourseRepository.class);
        EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
        AssessmentRepository assessments = mock(AssessmentRepository.class);
        AttendanceSessionRepository sessions = mock(AttendanceSessionRepository.class);
        FacultyCourseAssignmentRepository assignments =
                mock(FacultyCourseAssignmentRepository.class);

        Course course = new Course();
        course.setCourseId(11L);
        when(courses.findById(11L)).thenReturn(Optional.of(course));
        when(enrollments.countByCourseCourseId(11L)).thenReturn(3L);

        CourseService service =
                new CourseService(courses, enrollments, assessments, sessions, assignments, mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteCourse(11L));
        verify(courses, never()).delete(course);
    }

    @Test
    void courseWithAssessmentsOrSessionsIsRefusedWithConflict() {
        CourseRepository courses = mock(CourseRepository.class);
        EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
        AssessmentRepository assessments = mock(AssessmentRepository.class);
        AttendanceSessionRepository sessions = mock(AttendanceSessionRepository.class);
        FacultyCourseAssignmentRepository assignments =
                mock(FacultyCourseAssignmentRepository.class);

        Course course = new Course();
        course.setCourseId(11L);
        when(courses.findById(11L)).thenReturn(Optional.of(course));
        when(assessments.countByCourseCourseId(11L)).thenReturn(1L);

        CourseService service =
                new CourseService(courses, enrollments, assessments, sessions, assignments, mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteCourse(11L));
        verify(courses, never()).delete(course);
    }

    @Test
    void departmentWithStudentsFacultyOrCoursesIsRefusedWithConflict() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        FacultyRepository faculty = mock(FacultyRepository.class);
        CourseRepository courses = mock(CourseRepository.class);

        Department department = new Department();
        department.setDepartmentId(4L);
        when(departments.findById(4L)).thenReturn(Optional.of(department));
        when(faculty.countByDepartmentDepartmentId(4L)).thenReturn(2L);

        DepartmentService service =
                new DepartmentService(departments, students, faculty, courses, mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteDepartment(4L));
        verify(departments, never()).delete(department);
    }

    @Test
    void studentWithEnrollmentsIsRefusedWithConflict() {
        StudentRepository students = mock(StudentRepository.class);
        EnrollmentRepository enrollments = mock(EnrollmentRepository.class);

        Student student = new Student();
        student.setStudentId(7L);
        when(students.findById(7L)).thenReturn(Optional.of(student));
        when(enrollments.countByStudentStudentId(7L)).thenReturn(5L);

        StudentService service = new StudentService(
                students, mock(UserRepository.class), mock(DepartmentRepository.class), enrollments, mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteStudent(7L));
        verify(students, never()).delete(student);
    }

    @Test
    void facultyWithAttendanceSessionsIsRefusedWithConflict() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);
        AttendanceSessionRepository sessions = mock(AttendanceSessionRepository.class);

        Faculty faculty = new Faculty();
        faculty.setFacultyId(9L);
        when(facultyRepository.findById(9L)).thenReturn(Optional.of(faculty));
        when(sessions.countByFacultyFacultyId(9L)).thenReturn(4L);

        FacultyService service = new FacultyService(
                facultyRepository, mock(UserRepository.class),
                mock(DepartmentRepository.class), sessions,
                mock(FacultyCourseAssignmentRepository.class), mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteFaculty(9L));
        verify(facultyRepository, never()).delete(faculty);
    }

    // ------------------------------------------------------------------
    // Deletes only when nothing points at the record
    // ------------------------------------------------------------------

    @Test
    void courseWithoutDependentsIsDeleted() {
        CourseRepository courses = mock(CourseRepository.class);

        Course course = new Course();
        course.setCourseId(11L);
        when(courses.findById(11L)).thenReturn(Optional.of(course));

        CourseService service = new CourseService(courses,
                mock(EnrollmentRepository.class),
                mock(AssessmentRepository.class),
                mock(AttendanceSessionRepository.class),
                mock(FacultyCourseAssignmentRepository.class), mock(AuditLogService.class));

        service.deleteCourse(11L);

        verify(courses).delete(course);
    }

    @Test
    void studentWithoutEnrollmentsIsDeleted() {
        StudentRepository students = mock(StudentRepository.class);

        Student student = new Student();
        student.setStudentId(7L);
        when(students.findById(7L)).thenReturn(Optional.of(student));

        StudentService service = new StudentService(students,
                mock(UserRepository.class),
                mock(DepartmentRepository.class),
                mock(EnrollmentRepository.class), mock(AuditLogService.class));

        service.deleteStudent(7L);

        verify(students).delete(student);
    }

    // ------------------------------------------------------------------
    // Missing records still answer 404
    // ------------------------------------------------------------------

    @Test
    void deletingAnUnknownResourceIsNotFound() {
        CourseRepository courses = mock(CourseRepository.class);
        when(courses.findById(404L)).thenReturn(Optional.empty());

        CourseService service = new CourseService(courses, mock(AuditLogService.class));

        assertThrows(
                com.campuscore.backend.exception.ResourceNotFoundException.class,
                () -> service.deleteCourse(404L));
    }

    // ------------------------------------------------------------------
    // Fail closed when dependency lookups are unavailable
    // ------------------------------------------------------------------

    @Test
    void courseDeleteIsRefusedWhenDependenciesCannotBeVerified() {
        CourseRepository courses = mock(CourseRepository.class);

        Course course = new Course();
        course.setCourseId(11L);
        when(courses.findById(11L)).thenReturn(Optional.of(course));

        CourseService service = new CourseService(courses, mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteCourse(11L));
        verify(courses, never()).delete(course);
    }

    @Test
    void studentDeleteIsRefusedWhenEnrollmentsCannotBeVerified() {
        StudentRepository students = mock(StudentRepository.class);

        Student student = new Student();
        student.setStudentId(7L);
        when(students.findById(7L)).thenReturn(Optional.of(student));

        StudentService service = new StudentService(
                students, mock(UserRepository.class), mock(DepartmentRepository.class), mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteStudent(7L));
        verify(students, never()).delete(student);
    }

    @Test
    void facultyDeleteIsRefusedWhenSessionsCannotBeVerified() {
        FacultyRepository facultyRepository = mock(FacultyRepository.class);

        Faculty faculty = new Faculty();
        faculty.setFacultyId(9L);
        when(facultyRepository.findById(9L)).thenReturn(Optional.of(faculty));

        FacultyService service = new FacultyService(facultyRepository,
                mock(UserRepository.class), mock(DepartmentRepository.class), mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteFaculty(9L));
        verify(facultyRepository, never()).delete(faculty);
    }

    @Test
    void departmentDeleteIsRefusedWhenDependenciesCannotBeVerified() {
        DepartmentRepository departments = mock(DepartmentRepository.class);

        Department department = new Department();
        department.setDepartmentId(4L);
        when(departments.findById(4L)).thenReturn(Optional.of(department));

        DepartmentService service = new DepartmentService(departments, mock(AuditLogService.class));

        assertThrows(ResourceConflictException.class, () -> service.deleteDepartment(4L));
        verify(departments, never()).delete(department);
    }
}
