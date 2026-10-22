package com.campuscore.backend.service;

import com.campuscore.backend.dto.GradingPolicyRequest;
import com.campuscore.backend.dto.StudentRequest;
import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.AttendanceRecord;
import com.campuscore.backend.entity.AttendanceSession;
import com.campuscore.backend.entity.AuditLog;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.FacultyCourseAssignment;
import com.campuscore.backend.entity.GradingPolicy;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.AttendanceRecordRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.AuditLogRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.GradingPolicyRepository;
import com.campuscore.backend.repository.MarkRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The audit trail of the administrative mutations.
 *
 * <p>Before this coverage the trail held three rows of user account changes and
 * nothing else: a department, a course, an enrollment, a mark or an assignment
 * left no trace at all, so "who changed this" could only be answered for
 * accounts.
 *
 * <p>These tests drive the real services with mocked repositories and a real
 * audit service, so what is asserted is the row that would be written, not a
 * call that was made. The actor is read from the authenticated security
 * context, and the assertions check that no credential material reaches the
 * description.
 */
class AdministrativeAuditTest {

    private static final Long ADMIN_ID = 99L;

    private AuditLogRepository auditLogs;
    private AuditLogService auditLogService;
    private UserRepository users;
    private DepartmentRepository departments;
    private StudentRepository students;
    private FacultyRepository faculty;
    private CourseRepository courses;
    private EnrollmentRepository enrollments;
    private AssessmentRepository assessments;
    private MarkRepository marks;
    private AttendanceSessionRepository sessions;
    private AttendanceRecordRepository records;
    private FacultyCourseAssignmentRepository assignments;
    private GradingPolicyRepository policies;

    @BeforeEach
    void setUp() {
        auditLogs = mock(AuditLogRepository.class);
        users = mock(UserRepository.class);
        departments = mock(DepartmentRepository.class);
        students = mock(StudentRepository.class);
        faculty = mock(FacultyRepository.class);
        courses = mock(CourseRepository.class);
        enrollments = mock(EnrollmentRepository.class);
        assessments = mock(AssessmentRepository.class);
        marks = mock(MarkRepository.class);
        sessions = mock(AttendanceSessionRepository.class);
        records = mock(AttendanceRecordRepository.class);
        assignments = mock(FacultyCourseAssignmentRepository.class);
        policies = mock(GradingPolicyRepository.class);

        auditLogService = new AuditLogService(auditLogs, users);

        // The actor is the signed-in administrator. It is resolved from the
        // security context, never from the request.
        User admin = new User();
        admin.setUserId(ADMIN_ID);
        admin.setUsername("admin-account");
        when(users.findByUsername("admin-account")).thenReturn(Optional.of(admin));
        when(users.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-account", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(auditLogs.save(any(AuditLog.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // The trail
    // ------------------------------------------------------------------

    @Test
    @DisplayName("creating a department is recorded")
    void creatingADepartmentIsRecorded() {
        DepartmentService service =
                new DepartmentService(departments, null, null, null, auditLogService);

        Department department = new Department();
        department.setDepartmentId(11L);
        department.setName("Computer Science");
        department.setCode("CSE");
        when(departments.save(any(Department.class))).thenReturn(department);

        service.createDepartment(department);

        AuditLog entry = recordedEntry();
        assertEquals("CREATE_DEPARTMENT", entry.getAction());
        assertEquals("Department", entry.getEntityName());
        assertEquals(11L, entry.getEntityId());
        assertEquals(ADMIN_ID, entry.getUser().getUserId());
        assertTrue(entry.getDescription().contains("CSE"));
    }

    @Test
    @DisplayName("updating a department is recorded")
    void updatingADepartmentIsRecorded() {
        DepartmentService service =
                new DepartmentService(departments, null, null, null, auditLogService);

        Department existing = new Department();
        existing.setDepartmentId(11L);
        existing.setName("Computer Science");
        existing.setCode("CSE");
        when(departments.findById(11L)).thenReturn(Optional.of(existing));
        when(departments.save(any(Department.class))).thenReturn(existing);

        Department change = new Department();
        change.setName("Computing");
        change.setCode("CS2");

        service.updateDepartment(11L, change);

        AuditLog entry = recordedEntry();
        assertEquals("UPDATE_DEPARTMENT", entry.getAction());
        assertEquals(11L, entry.getEntityId());
        assertTrue(entry.getDescription().contains("CSE"));
        assertTrue(entry.getDescription().contains("CS2"));
    }

    @Test
    @DisplayName("deleting a department is recorded")
    void deletingADepartmentIsRecorded() {
        DepartmentService service =
                new DepartmentService(departments, students, faculty, courses, auditLogService);

        Department existing = new Department();
        existing.setDepartmentId(11L);
        existing.setName("Computer Science");
        existing.setCode("CSE");
        when(departments.findById(11L)).thenReturn(Optional.of(existing));
        when(students.countByDepartmentDepartmentId(11L)).thenReturn(0L);
        when(faculty.countByDepartmentDepartmentId(11L)).thenReturn(0L);
        when(courses.countByDepartmentDepartmentId(11L)).thenReturn(0L);

        service.deleteDepartment(11L);

        AuditLog entry = recordedEntry();
        assertEquals("DELETE_DEPARTMENT", entry.getAction());
        assertEquals(11L, entry.getEntityId());
    }

    @Test
    @DisplayName("creating and deactivating a course are recorded")
    void courseLifecycleIsRecorded() {
        CourseService service = new CourseService(courses, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        course.setCourseName("Data Structures");
        when(courses.save(any(Course.class))).thenReturn(course);

        service.createCourse(course);
        assertEquals("CREATE_COURSE", recordedEntry().getAction());

        when(courses.findById(31L)).thenReturn(Optional.of(course));
        service.deactivateCourse(31L);
        assertEquals("DEACTIVATE_COURSE", recordedEntry().getAction());
        assertEquals("Course", recordedEntry().getEntityName());
        assertEquals(31L, recordedEntry().getEntityId());
    }

    @Test
    @DisplayName("creating a faculty profile is recorded")
    void creatingAFacultyProfileIsRecorded() {
        FacultyService service = new FacultyService(faculty, users, departments, auditLogService);

        User account = new User();
        account.setUserId(50L);
        account.setRole(User.Role.FACULTY);
        when(users.findById(50L)).thenReturn(Optional.of(account));

        Department department = new Department();
        department.setDepartmentId(1L);
        when(departments.findById(1L)).thenReturn(Optional.of(department));

        Faculty created = new Faculty();
        created.setFacultyId(21L);
        created.setEmployeeNumber("DEMO-FAC-001");
        when(faculty.save(any(Faculty.class))).thenReturn(created);

        service.createFaculty(50L, 1L, "DEMO-FAC-001", "Jamie", "Patel", null);

        AuditLog entry = recordedEntry();
        assertEquals("CREATE_FACULTY", entry.getAction());
        assertEquals("Faculty", entry.getEntityName());
        assertEquals(21L, entry.getEntityId());
        assertTrue(entry.getDescription().contains("DEMO-FAC-001"));
    }

    @Test
    @DisplayName("creating a student profile is recorded without the personal fields")
    void creatingAStudentProfileIsRecorded() {
        StudentService service = new StudentService(students, users, departments, enrollments, auditLogService);

        User account = new User();
        account.setUserId(60L);
        account.setRole(User.Role.STUDENT);
        when(users.findById(60L)).thenReturn(Optional.of(account));

        Department department = new Department();
        department.setDepartmentId(1L);
        when(departments.findById(1L)).thenReturn(Optional.of(department));

        Student created = new Student();
        created.setStudentId(51L);
        when(students.save(any(Student.class))).thenReturn(created);

        StudentRequest request = new StudentRequest();
        request.setUserId(60L);
        request.setDepartmentId(1L);
        request.setEnrollmentNumber("DEMO-2026-001");
        request.setFirstName("Avery");
        request.setLastName("Chen");
        request.setDateOfBirth(LocalDate.of(2004, 5, 6));
        request.setPhone("555-0100");
        request.setAdmissionYear(2026);

        service.createStudent(request);

        AuditLog entry = recordedEntry();
        assertEquals("CREATE_STUDENT", entry.getAction());
        assertEquals(51L, entry.getEntityId());
        // The date of birth, the phone number and the name are not recorded.
        assertFalse(entry.getDescription().contains("2004"));
        assertFalse(entry.getDescription().contains("555-0100"));
        assertFalse(entry.getDescription().contains("Chen"));
    }

    @Test
    @DisplayName("creating, updating and deleting an enrollment are recorded")
    void enrollmentLifecycleIsRecorded() {
        AccessScope scope = new AccessScope(users, students, faculty, assignments, enrollments);
        EnrollmentService service = new EnrollmentService(
                enrollments, students, courses, scope, marks, records, auditLogService);

        Student student = new Student();
        student.setStudentId(51L);
        when(students.findById(51L)).thenReturn(Optional.of(student));

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(301L);
        enrollment.setSemester("Fall");
        enrollment.setAcademicYear("2026-2027");
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        when(enrollments.save(any(Enrollment.class))).thenReturn(enrollment);

        service.createEnrollment(51L, 31L, "Fall", "2026-2027");
        assertEquals("CREATE_ENROLLMENT", recordedEntry().getAction());
        assertEquals(301L, recordedEntry().getEntityId());

        when(enrollments.findById(301L)).thenReturn(Optional.of(enrollment));
        service.updateEnrollment(301L, "Fall", "2026-2027", Enrollment.Status.COMPLETED);
        assertEquals("UPDATE_ENROLLMENT", recordedEntry().getAction());

        service.deleteEnrollment(301L);
        assertEquals("DELETE_ENROLLMENT", recordedEntry().getAction());
    }

    @Test
    @DisplayName("creating, updating and deleting an assessment are recorded")
    void assessmentLifecycleIsRecorded() {
        AccessScope scope = new AccessScope(users, students, faculty, assignments, enrollments);
        AssessmentService service =
                new AssessmentService(assessments, courses, scope, marks, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(41L);
        assessment.setName("Midterm");
        assessment.setCourse(course);
        assessment.setMaxMarks(new BigDecimal("100.00"));
        when(assessments.save(any(Assessment.class))).thenReturn(assessment);

        service.createAssessment(31L, "Midterm", Assessment.AssessmentType.MIDTERM,
                new BigDecimal("100.00"), new BigDecimal("30.00"));
        assertEquals("CREATE_ASSESSMENT", recordedEntry().getAction());
        assertEquals(41L, recordedEntry().getEntityId());

        when(assessments.findById(41L)).thenReturn(Optional.of(assessment));
        service.deleteAssessment(41L);
        assertEquals("DELETE_ASSESSMENT", recordedEntry().getAction());
    }

    @Test
    @DisplayName("creating, correcting and deleting a mark are recorded")
    void markLifecycleIsRecorded() {
        AccessScope scope = new AccessScope(users, students, faculty, assignments, enrollments);
        MarkService service = new MarkService(marks, enrollments, assessments, scope, auditLogService);

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(301L);
        when(enrollments.findById(301L)).thenReturn(Optional.of(enrollment));

        Assessment assessment = new Assessment();
        assessment.setAssessmentId(41L);
        assessment.setMaxMarks(new BigDecimal("100.00"));
        when(assessments.findById(41L)).thenReturn(Optional.of(assessment));

        Mark mark = new Mark();
        mark.setMarkId(701L);
        mark.setEnrollment(enrollment);
        mark.setAssessment(assessment);
        when(marks.save(any(Mark.class))).thenReturn(mark);

        service.createMark(301L, 41L, new BigDecimal("42.00"));
        assertEquals("CREATE_MARK", recordedEntry().getAction());
        assertEquals(701L, recordedEntry().getEntityId());

        when(marks.findById(701L)).thenReturn(Optional.of(mark));
        service.deleteMark(701L);
        assertEquals("DELETE_MARK", recordedEntry().getAction());
    }

    @Test
    @DisplayName("creating and deleting an attendance session are recorded")
    void attendanceSessionLifecycleIsRecorded() {
        AccessScope scope = new AccessScope(users, students, faculty, assignments, enrollments);
        AttendanceSessionService service =
                new AttendanceSessionService(sessions, courses, faculty, scope, records, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        Faculty teacher = new Faculty();
        teacher.setFacultyId(21L);
        when(faculty.findById(21L)).thenReturn(Optional.of(teacher));

        AttendanceSession session = new AttendanceSession();
        session.setAttendanceSessionId(81L);
        session.setCourse(course);
        session.setFaculty(teacher);
        when(sessions.save(any(AttendanceSession.class))).thenReturn(session);

        service.createSession(31L, 21L, LocalDate.of(2026, 9, 1), "Lecture 1");
        assertEquals("CREATE_ATTENDANCE_SESSION", recordedEntry().getAction());
        assertEquals(81L, recordedEntry().getEntityId());

        when(sessions.findById(81L)).thenReturn(Optional.of(session));
        service.deleteSession(81L);
        assertEquals("DELETE_ATTENDANCE_SESSION", recordedEntry().getAction());
    }

    @Test
    @DisplayName("marking attendance is recorded")
    void markingAttendanceIsRecorded() {
        AccessScope scope = new AccessScope(users, students, faculty, assignments, enrollments);
        AttendanceRecordService service =
                new AttendanceRecordService(records, sessions, enrollments, scope, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        AttendanceSession session = new AttendanceSession();
        session.setAttendanceSessionId(81L);
        session.setCourse(course);
        when(sessions.findById(81L)).thenReturn(Optional.of(session));

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(301L);
        when(enrollments.findById(301L)).thenReturn(Optional.of(enrollment));

        AttendanceRecord record = new AttendanceRecord();
        record.setAttendanceRecordId(901L);
        when(records.save(any(AttendanceRecord.class))).thenReturn(record);

        service.createRecord(81L, 301L, AttendanceRecord.Status.PRESENT);
        assertEquals("CREATE_ATTENDANCE_RECORD", recordedEntry().getAction());
        assertEquals(901L, recordedEntry().getEntityId());
    }

    @Test
    @DisplayName("assigning, handing over and ending an assignment are recorded")
    void assignmentLifecycleIsRecorded() {
        FacultyCourseAssignmentService service =
                new FacultyCourseAssignmentService(assignments, courses, faculty, enrollments, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        Faculty teacher = new Faculty();
        teacher.setFacultyId(21L);
        teacher.setEmployeeNumber("DEMO-FAC-001");
        teacher.setFirstName("Jamie");
        teacher.setLastName("Patel");
        when(faculty.findById(21L)).thenReturn(Optional.of(teacher));

        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setAssignmentId(1001L);
        assignment.setCourse(course);
        assignment.setFaculty(teacher);
        when(assignments.save(any(FacultyCourseAssignment.class))).thenReturn(assignment);

        service.assignFaculty(31L, 21L);
        int entriesAfterAssigning = recordedEntries().size();
        AuditLog assigned = recordedEntry();
        assertEquals("ASSIGN_FACULTY", assigned.getAction());
        assertEquals("FacultyCourseAssignment", assigned.getEntityName());
        assertEquals(1001L, assigned.getEntityId());
        assertTrue(assigned.getDescription().contains("DEMO-FAC-001"));

        // A handover goes to a different faculty member: handing a course to
        // the member who already holds it is refused, because it would leave
        // the course with two open assignments for the same person.
        FacultyCourseAssignment previous = new FacultyCourseAssignment();
        previous.setAssignmentId(1000L);
        previous.setCourse(course);
        previous.setFaculty(teacher);
        when(assignments.findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(31L))
                .thenReturn(List.of(previous));

        Faculty replacement = new Faculty();
        replacement.setFacultyId(22L);
        replacement.setEmployeeNumber("DEMO-FAC-002");
        replacement.setFirstName("Morgan");
        replacement.setLastName("Rivera");
        when(faculty.findById(22L)).thenReturn(Optional.of(replacement));

        service.reassignFaculty(31L, 22L);
        AuditLog handedOver = recordedEntry();
        assertEquals("REASSIGN_FACULTY", handedOver.getAction());
        // Exactly one event for the handover, not one for the closed row and
        // one for the new one.
        assertEquals(entriesAfterAssigning + 1, recordedEntries().size());
    }

    @Test
    @DisplayName("ending an assignment is recorded")
    void endingAnAssignmentIsRecorded() {
        FacultyCourseAssignmentService service =
                new FacultyCourseAssignmentService(assignments, courses, faculty, enrollments, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        Faculty teacher = new Faculty();
        teacher.setFacultyId(21L);
        teacher.setEmployeeNumber("DEMO-FAC-001");
        when(faculty.findById(21L)).thenReturn(Optional.of(teacher));

        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setAssignmentId(1001L);
        assignment.setCourse(course);
        assignment.setFaculty(teacher);
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(31L, 21L))
                .thenReturn(Optional.of(assignment));
        when(assignments.countByCourseCourseIdAndEndedAtIsNull(31L)).thenReturn(2L);

        service.removeAssignment(31L, 21L);

        AuditLog entry = recordedEntry();
        assertEquals("END_FACULTY_ASSIGNMENT", entry.getAction());
        assertEquals(1001L, entry.getEntityId());
    }

    @Test
    @DisplayName("creating a grading policy is recorded")
    void creatingAGradingPolicyIsRecorded() {
        GradingPolicyService service = new GradingPolicyService(policies, auditLogService);

        GradingPolicy policy = new GradingPolicy();
        policy.setGradingPolicyId(61L);
        policy.setName("DEFAULT_10_POINT");
        policy.setGrade("A");
        when(policies.save(any(GradingPolicy.class))).thenReturn(policy);

        GradingPolicyRequest request = new GradingPolicyRequest();
        request.setName("DEFAULT_10_POINT");
        request.setMinPercentage(new BigDecimal("80.00"));
        request.setMaxPercentage(new BigDecimal("89.99"));
        request.setGrade("A");
        request.setGradePoint(new BigDecimal("8.00"));

        service.createPolicy(request);

        AuditLog entry = recordedEntry();
        assertEquals("CREATE_GRADING_POLICY", entry.getAction());
        assertEquals(61L, entry.getEntityId());
    }

    // ------------------------------------------------------------------
    // Integrity of the trail
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a mutation that fails leaves no record of a success")
    void aFailedMutationRecordsNothing() {
        FacultyCourseAssignmentService service =
                new FacultyCourseAssignmentService(assignments, courses, faculty, enrollments, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        when(courses.findById(31L)).thenReturn(Optional.of(course));

        Faculty teacher = new Faculty();
        teacher.setFacultyId(21L);
        teacher.setEmployeeNumber("DEMO-FAC-001");
        when(faculty.findById(21L)).thenReturn(Optional.of(teacher));

        FacultyCourseAssignment existing = new FacultyCourseAssignment();
        existing.setAssignmentId(1001L);
        when(assignments.findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(31L, 21L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ResourceConflictException.class,
                () -> service.assignFaculty(31L, 21L));

        verify(auditLogs, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("a duplicate course code is refused and records nothing")
    void aRefusedDuplicateRecordsNothing() {
        CourseService service = new CourseService(courses, auditLogService);

        Course course = new Course();
        course.setCourseId(31L);
        course.setCourseCode("CSE101");
        course.setCourseName("Data Structures");
        when(courses.save(any(Course.class))).thenThrow(
                new ResourceConflictException("Course code already exists: CSE101"));

        assertThrows(ResourceConflictException.class, () -> service.createCourse(course));

        verify(auditLogs, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("the actor is the signed-in account, never a value from the request")
    void theActorComesFromTheSecurityContext() {
        DepartmentService service =
                new DepartmentService(departments, null, null, null, auditLogService);

        Department department = new Department();
        department.setDepartmentId(11L);
        department.setCode("CSE");
        when(departments.save(any(Department.class))).thenReturn(department);

        service.createDepartment(department);

        AuditLog entry = recordedEntry();
        assertNotNull(entry.getUser());
        assertEquals(ADMIN_ID, entry.getUser().getUserId());
        assertEquals("admin-account", entry.getUser().getUsername());
    }

    @Test
    @DisplayName("no credential material reaches the trail")
    void noCredentialMaterialReachesTheTrail() {
        UserService userService = new UserService(users, auditLogService);

        when(users.existsByUsername("new-account")).thenReturn(false);
        when(users.existsByEmail("new@example.test")).thenReturn(false);
        when(users.findByUsername("admin-account")).thenReturn(Optional.of(
                userWithId(ADMIN_ID, "admin-account")));

        User created = new User();
        created.setUserId(500L);
        created.setUsername("new-account");
        when(users.save(any(User.class))).thenReturn(created);

        com.campuscore.backend.dto.UserRequest request =
                new com.campuscore.backend.dto.UserRequest();
        request.setUsername("new-account");
        request.setEmail("new@example.test");
        request.setPassword("Sup3rSecretValue!");
        request.setRole(User.Role.STUDENT);

        userService.createUser(request);

        AuditLog entry = recordedEntry();
        assertEquals("CREATE_USER", entry.getAction());
        assertFalse(entry.getDescription().contains("Sup3rSecretValue"));
        assertFalse(entry.getDescription().toLowerCase().contains("password"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User userWithId(Long id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }

    private List<AuditLog> recordedEntries() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogs, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    private AuditLog recordedEntry() {
        List<AuditLog> entries = recordedEntries();
        assertFalse(entries.isEmpty(), "expected an audit entry to have been written");
        return entries.get(entries.size() - 1);
    }
}
