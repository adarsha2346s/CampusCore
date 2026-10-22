package com.campuscore.backend.controller;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.GlobalExceptionHandler;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.AuditLogService;
import com.campuscore.backend.service.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The faculty student directory must be scoped on the paginated route.
 *
 * <p>The unpaged listing only ever returned the students a faculty member
 * teaches. The paginated listing did not: it went straight to the whole
 * student table, so adding {@code ?page=0&size=100} to the faculty directory
 * URL returned the institution's entire student directory, including names,
 * dates of birth, phone numbers and enrollment numbers.
 *
 * <p>These tests drive the real controller, the real service and the real
 * AccessScope, with only the repositories mocked, and they assert both what
 * the caller receives and which query the service actually ran. The last
 * assertion matters: a fix that only trimmed the response after the fact would
 * still have executed the global query.
 */
class FacultyStudentDirectoryScopeTest {

    private static final Long TEACHING_COURSE_ID = 31L;
    private static final Long FACULTY_ID = 41L;
    private static final Long TAUGHT_STUDENT_ID = 51L;
    private static final Long OTHER_STUDENT_ID = 52L;

    private StudentRepository students;
    private UserRepository users;
    private FacultyRepository faculty;
    private FacultyCourseAssignmentRepository assignments;
    private EnrollmentRepository enrollments;
    private StudentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        students = mock(StudentRepository.class);
        users = mock(UserRepository.class);
        faculty = mock(FacultyRepository.class);
        assignments = mock(FacultyCourseAssignmentRepository.class);
        enrollments = mock(EnrollmentRepository.class);

        AccessScope accessScope = new AccessScope(
                users, students, faculty, assignments, enrollments);

        service = new StudentService(
                students,
                users,
                mock(DepartmentRepository.class),
                enrollments,
                accessScope, mock(AuditLogService.class));

        StudentController controller = new StudentController(
                service, students, users, accessScope);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Faculty
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a paginated faculty listing returns only the students the caller teaches")
    void paginatedFacultyListingIsScopedToTheTaughtStudents() throws Exception {
        asFacultyAssignedTo(TEACHING_COURSE_ID);
        scopedPageReturnsOnlyTaughtStudents();

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].studentId").value(TAUGHT_STUDENT_ID));
    }

    @Test
    @DisplayName("a paginated faculty listing never runs the institution-wide query")
    void paginatedFacultyListingDoesNotQueryTheWholeDirectory() {
        asFacultyAssignedTo(TEACHING_COURSE_ID);
        scopedPageReturnsOnlyTaughtStudents();

        service.getStudentsPage(Pageable.unpaged());

        verify(students, never()).findAll(any(Pageable.class));
        verify(students).findDistinctByCourseCourseIdIn(
                anyCollection(), any(Pageable.class));
    }

    @Test
    @DisplayName("the reported total is the scoped total, not the size of the directory")
    void paginatedFacultyListingReportsAScopedTotal() throws Exception {
        asFacultyAssignedTo(TEACHING_COURSE_ID);

        // The institution holds more students than the caller may see.
        when(students.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taughtStudent(), otherStudent())));
        when(students.findDistinctByCourseCourseIdIn(anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taughtStudent()), pageableOf(), 1));

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("a faculty member without an assignment gets an empty page")
    void facultyWithoutAssignmentsGetsAnEmptyPage() throws Exception {
        asFacultyWithoutAssignments();
        when(students.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taughtStudent(), otherStudent())));

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));

        verify(students, never()).findAll(any(Pageable.class));
        verify(students, never()).findDistinctByCourseCourseIdIn(
                anyCollection(), any(Pageable.class));
    }

    @Test
    @DisplayName("a faculty member cannot read a student they do not teach through the page")
    void facultyCannotReadUnrelatedStudentsThroughThePage() throws Exception {
        asFacultyAssignedTo(TEACHING_COURSE_ID);
        scopedPageReturnsOnlyTaughtStudents();

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.studentId == " + OTHER_STUDENT_ID + ")]")
                        .isEmpty());
    }

    // ------------------------------------------------------------------
    // Admin
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an administrator still sees the whole directory")
    void adminSeesTheWholeDirectory() throws Exception {
        asAdmin();
        when(students.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taughtStudent(), otherStudent())));

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(students).findAll(any(Pageable.class));
        verify(students, never()).findDistinctByCourseCourseIdIn(
                anyCollection(), any(Pageable.class));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void scopedPageReturnsOnlyTaughtStudents() {
        // The whole directory is what an unscoped query would return. Stubbing it
        // here means a regression shows up as a listing that contains too many
        // students, rather than as a mock returning null.
        when(students.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taughtStudent(), otherStudent())));
        when(students.findDistinctByCourseCourseIdIn(anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taughtStudent()), pageableOf(), 1));
    }

    private Pageable pageableOf() {
        return Pageable.ofSize(10);
    }

    private void asFacultyAssignedTo(long courseId) {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of(assignment(courseId)));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));
    }

    private void asFacultyWithoutAssignments() {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        when(assignments.findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(FACULTY_ID))
                .thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));
    }

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private com.campuscore.backend.entity.FacultyCourseAssignment assignment(long courseId) {
        Course course = new Course();
        course.setCourseId(courseId);
        course.setCourseCode("CSE" + courseId);

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);

        com.campuscore.backend.entity.FacultyCourseAssignment assignment =
                new com.campuscore.backend.entity.FacultyCourseAssignment();
        assignment.setCourse(course);
        assignment.setFaculty(me);
        return assignment;
    }

    private Student taughtStudent() {
        return student(TAUGHT_STUDENT_ID, "DEMO-2026-001", "Avery", "Chen");
    }

    private Student otherStudent() {
        return student(OTHER_STUDENT_ID, "DEMO-2026-002", "Riley", "Brooks");
    }

    private Student student(Long id, String enrollmentNumber, String first, String last) {
        Department department = new Department();
        department.setDepartmentId(7L);
        department.setName("Computer Science");
        department.setCode("CSE");

        User owner = new User();
        owner.setUserId(id + 1000L);
        owner.setUsername("student-" + id);
        owner.setEmail("student-" + id + "@example.test");
        owner.setRole(User.Role.STUDENT);

        Student student = new Student();
        student.setStudentId(id);
        student.setUser(owner);
        student.setDepartment(department);
        student.setEnrollmentNumber(enrollmentNumber);
        student.setFirstName(first);
        student.setLastName(last);
        student.setDateOfBirth(LocalDate.of(2004, 1, 1));
        student.setPhone("000");
        student.setAdmissionYear(2026);
        student.setStatus(Student.Status.ACTIVE);
        return student;
    }
}
