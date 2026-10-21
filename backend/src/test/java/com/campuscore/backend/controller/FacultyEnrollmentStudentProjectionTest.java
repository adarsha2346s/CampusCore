package com.campuscore.backend.controller;

import com.campuscore.backend.dto.EnrollmentResponse;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.AccessScope;
import com.campuscore.backend.service.EnrollmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Faculty-scoped enrollment -> student projection.
 *
 * <p>A faculty member reading the enrollments of a course they teach needs to see
 * who is enrolled, not a bare enrollment id. The projection must stay inside the
 * course assignment: the faculty directory route is deliberately not used here.
 */
class FacultyEnrollmentStudentProjectionTest {

    private static final Long COURSE_ID = 11L;
    private static final Long UNASSIGNED_COURSE_ID = 99L;
    private static final Long FACULTY_ID = 21L;
    private static final Long STUDENT_ID = 7L;
    private static final Long ENROLLMENT_ID = 3000L;

    private EnrollmentRepository enrollments;
    private StudentRepository students;
    private FacultyRepository faculty;
    private FacultyCourseAssignmentRepository assignments;
    private UserRepository users;
    private EnrollmentService service;
    private EnrollmentController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentRepository.class);
        students = mock(StudentRepository.class);
        faculty = mock(FacultyRepository.class);
        assignments = mock(FacultyCourseAssignmentRepository.class);
        users = mock(UserRepository.class);

        service = new EnrollmentService(
                enrollments,
                students,
                mock(CourseRepository.class),
                new AccessScope(users, students, faculty, assignments, enrollments));

        controller = new EnrollmentController(service, mock(com.campuscore.backend.service.StudentService.class));
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new com.campuscore.backend.exception.GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void asFacultyAssignedTo(long courseId) {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));

        when(assignments.existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(courseId, FACULTY_ID))
                .thenReturn(courseId == COURSE_ID);

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

    private Enrollment enrollment() {
        Course course = new Course();
        course.setCourseId(COURSE_ID);
        course.setCourseCode("CSE101");

        Student owner = new Student();
        owner.setStudentId(STUDENT_ID);
        owner.setEnrollmentNumber("DEMO-2026-001");
        owner.setFirstName("Avery");
        owner.setLastName("Chen");

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(ENROLLMENT_ID);
        enrollment.setStudent(owner);
        enrollment.setCourse(course);
        enrollment.setSemester("Fall");
        enrollment.setAcademicYear("2026-2027");
        enrollment.setEnrollmentDate(LocalDate.of(2026, 8, 10));
        enrollment.setStatus(Enrollment.Status.ENROLLED);
        return enrollment;
    }

    // ------------------------------------------------------------------
    // Inside the teaching scope
    // ------------------------------------------------------------------

    @Test
    void facultyAssignedToTheCourseSeesTheStudentIdentity() throws Exception {
        asFacultyAssignedTo(COURSE_ID);
        when(enrollments.findByCourseCourseId(COURSE_ID)).thenReturn(List.of(enrollment()));

        mockMvc.perform(get("/api/v1/enrollments/course/" + COURSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentFirstName").value("Avery"))
                .andExpect(jsonPath("$[0].studentLastName").value("Chen"))
                .andExpect(jsonPath("$[0].enrollmentNumber").value("DEMO-2026-001"))
                .andExpect(jsonPath("$[0].studentId").value(STUDENT_ID));
    }

    @Test
    void theProjectionStillCarriesTheExistingEnrollmentFields() throws Exception {
        asFacultyAssignedTo(COURSE_ID);
        when(enrollments.findByCourseCourseId(COURSE_ID)).thenReturn(List.of(enrollment()));

        mockMvc.perform(get("/api/v1/enrollments/course/" + COURSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$[0].courseId").value(COURSE_ID))
                .andExpect(jsonPath("$[0].semester").value("Fall"))
                .andExpect(jsonPath("$[0].academicYear").value("2026-2027"))
                .andExpect(jsonPath("$[0].enrollmentDate").value("2026-08-10"))
                .andExpect(jsonPath("$[0].status").value("ENROLLED"));
    }

    // ------------------------------------------------------------------
    // Outside the teaching scope
    // ------------------------------------------------------------------

    @Test
    void facultyOutsideTheTeachingScopeGetNoEnrollmentsAtAll() throws Exception {
        asFacultyAssignedTo(COURSE_ID);

        // The unassigned course is refused before any enrollment is read.
        mockMvc.perform(get("/api/v1/enrollments/course/" + UNASSIGNED_COURSE_ID))
                .andExpect(status().isForbidden());

        verify(enrollments, never()).findByCourseCourseId(UNASSIGNED_COURSE_ID);
    }

    @Test
    void theServiceRefusesAnUnassignedCourse() {
        asFacultyAssignedTo(COURSE_ID);

        assertThrows(AccessDeniedException.class,
                () -> service.getEnrollmentsByCourse(UNASSIGNED_COURSE_ID));

        verify(enrollments, never()).findByCourseCourseId(anyLong());
    }

    @Test
    void aClosedAssignmentDoesNotRevealTheStudent() {
        // The assignment exists but has ended, so course access is refused.
        User account = new User();
        account.setUserId(1L);
        account.setUsername("faculty-account");
        when(users.findByUsername("faculty-account")).thenReturn(Optional.of(account));

        Faculty me = new Faculty();
        me.setFacultyId(FACULTY_ID);
        when(faculty.findByUserUserId(1L)).thenReturn(Optional.of(me));
        when(assignments.existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(COURSE_ID, FACULTY_ID))
                .thenReturn(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "faculty-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))));

        assertThrows(AccessDeniedException.class, () -> service.getEnrollmentsByCourse(COURSE_ID));
        verify(enrollments, never()).findByCourseCourseId(anyLong());
    }

    // ------------------------------------------------------------------
    // Administrator and student behaviour unchanged
    // ------------------------------------------------------------------

    @Test
    void administratorsKeepGlobalEnrollmentRead() throws Exception {
        asAdmin();
        when(enrollments.findAll()).thenReturn(List.of(enrollment()));
        when(enrollments.findByCourseCourseId(COURSE_ID)).thenReturn(List.of(enrollment()));

        mockMvc.perform(get("/api/v1/enrollments/course/" + COURSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentFirstName").value("Avery"));
    }

    @Test
    void aStudentStillOnlySeesTheirOwnEnrollments() throws Exception {
        User account = new User();
        account.setUserId(1L);
        account.setUsername("student-account");
        when(users.findByUsername("student-account")).thenReturn(Optional.of(account));

        Student me = new Student();
        me.setStudentId(STUDENT_ID);
        when(students.findByUserUserId(1L)).thenReturn(Optional.of(me));

        var studentService = mock(com.campuscore.backend.service.StudentService.class);
        when(studentService.getStudentForUsername("student-account")).thenReturn(me);
        when(enrollments.findByStudentStudentId(STUDENT_ID)).thenReturn(List.of(enrollment()));

        MockMvc studentMockMvc = MockMvcBuilders
                .standaloneSetup(new EnrollmentController(service, studentService))
                .setControllerAdvice(new com.campuscore.backend.exception.GlobalExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        // Unchanged: only their own enrollments, now also carrying their own name.
        studentMockMvc.perform(get("/api/v1/enrollments/me")
                        .principal(new UsernamePasswordAuthenticationToken(
                                "student-account", "n/a",
                                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$[0].studentFirstName").value("Avery"));
    }

    // ------------------------------------------------------------------
    // The projection is built from the enrollment's own student
    // ------------------------------------------------------------------

    @Test
    void theStudentIdentityComesFromTheEnrollmentNotFromTheRequest() {
        asFacultyAssignedTo(COURSE_ID);
        when(enrollments.findByCourseCourseId(COURSE_ID)).thenReturn(List.of(enrollment()));

        // The controller maps the enrollment's own student; nothing is taken from
        // the request, so there is no identifier a caller could substitute.
        List<EnrollmentResponse> response =
                controller.getEnrollmentsByCourse(COURSE_ID).getBody();

        assertEquals(1, response.size());
        assertEquals("Avery", response.get(0).getStudentFirstName());
        assertEquals("Chen", response.get(0).getStudentLastName());
        assertEquals("DEMO-2026-001", response.get(0).getEnrollmentNumber());
        assertEquals(STUDENT_ID, response.get(0).getStudentId());
    }
}
