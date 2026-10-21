package com.campuscore.backend.service;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Single source of truth for "who is acting" and "what may they touch".
 *
 * <p>Spring Security decides <em>which roles</em> may reach an endpoint. This
 * component decides <em>which records</em> that role may act on, which the
 * request matcher cannot express because it only sees the path. Both layers are
 * required: hiding a route in the frontend is never a control.
 *
 * <p>The acting principal always comes from the verified {@link SecurityContext},
 * which is populated by the JWT filter from a signature-checked token, and whose
 * authorities are re-read from the database on every request. Role changes and
 * account deactivation therefore take effect immediately instead of waiting for
 * a token to expire.
 *
 * <p>Scope rules:
 * <ul>
 *   <li>ADMIN reaches everything.</li>
 *   <li>FACULTY reaches a course only while assigned to it.</li>
 *   <li>STUDENT reaches only their own enrollments, marks, attendance and profile.</li>
 * </ul>
 */
@Service
public class AccessScope {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyCourseAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AccessScope(
            UserRepository userRepository,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            FacultyCourseAssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository) {

        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    public Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean isAuthenticated() {
        Authentication authentication = currentAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    public boolean hasRole(String role) {
        Authentication authentication = currentAuthentication();

        if (authentication == null || !isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isFaculty() {
        return hasRole("FACULTY");
    }

    public boolean isStudent() {
        return hasRole("STUDENT");
    }

    public Optional<User> currentUser() {
        Authentication authentication = currentAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }

        return userRepository.findByUsername(authentication.getName());
    }

    public Optional<Student> currentStudent() {
        return currentUser()
                .flatMap(user -> studentRepository.findByUserUserId(user.getUserId()));
    }

    public Optional<Faculty> currentFaculty() {
        return currentUser()
                .flatMap(user -> facultyRepository.findByUserUserId(user.getUserId()));
    }

    // ------------------------------------------------------------------
    // Scope predicates
    // ------------------------------------------------------------------

    /** True when the acting faculty member currently teaches the course. */
    public boolean isAssignedToCourse(Long courseId) {
        if (!isFaculty()) {
            return false;
        }

        return currentFaculty()
                .map(faculty -> assignmentRepository
                        .existsByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(
                                courseId, faculty.getFacultyId()))
                .orElse(false);
    }

    public List<Long> assignedCourseIds() {
        if (!isFaculty()) {
            return List.of();
        }

        return currentFaculty()
                .map(faculty -> assignmentRepository
                        .findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(
                                faculty.getFacultyId())
                        .stream()
                        .map(assignment -> assignment.getCourse().getCourseId())
                        .toList())
                .orElse(List.of());
    }

    public boolean ownsStudent(Long studentId) {
        if (!isStudent()) {
            return false;
        }

        return currentStudent()
                .map(student -> student.getStudentId().equals(studentId))
                .orElse(false);
    }

    public boolean ownsEnrollment(Long enrollmentId) {
        if (!isStudent()) {
            return false;
        }

        return currentStudent()
                .map(student -> enrollmentRepository
                        .existsByStudentStudentId(student.getStudentId(), enrollmentId))
                .orElse(false);
    }

    /** True when the acting faculty member teaches a course the student is enrolled in. */
    public boolean teachesStudent(Long studentId) {
        if (!isFaculty()) {
            return false;
        }

        List<Long> courseIds = assignedCourseIds();
        if (courseIds.isEmpty()) {
            return false;
        }

        return enrollmentRepository.existsByStudentStudentIdAndCourseCourseIdIn(studentId, courseIds);
    }

    // ------------------------------------------------------------------
    // Guards. These fail closed: anything that cannot be resolved is refused.
    // ------------------------------------------------------------------

    /** ADMIN, or the faculty member currently assigned to the course. */
    public void requireCourseAccess(Long courseId) {
        if (isAdmin() || isAssignedToCourse(courseId)) {
            return;
        }

        throw new AccessDeniedException(
                "You are not assigned to this course");
    }

    /**
     * ADMIN, the faculty member assigned to the enrollment's course, or the
     * student who owns the enrollment.
     */
    public void requireEnrollmentAccess(Long enrollmentId) {
        if (isAdmin()) {
            return;
        }

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);

        if (enrollment == null) {
            throw new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId);
        }

        // Fail closed rather than dereferencing a partially populated graph.
        if (enrollment.getCourse() == null || enrollment.getStudent() == null) {
            throw new AccessDeniedException("You cannot access this enrollment");
        }

        Long courseId = enrollment.getCourse().getCourseId();

        if (isAssignedToCourse(courseId)) {
            return;
        }

        if (isStudent()
                && enrollment.getStudent().getStudentId().equals(currentStudentIdOrThrow())) {

            return;
        }

        throw new AccessDeniedException("You cannot access this enrollment");
    }

    /**
     * ADMIN, the student themselves, or a faculty member teaching that student.
     */
    public void requireStudentAccess(Long studentId) {
        if (isAdmin() || ownsStudent(studentId) || teachesStudent(studentId)) {
            return;
        }

        // Refused without stating whether the student exists.
        throw new AccessDeniedException("You cannot access this student record");
    }

    public Long currentStudentIdOrThrow() {
        return currentStudent()
                .map(Student::getStudentId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No student profile is linked to this account"));
    }

    public Long currentFacultyIdOrThrow() {
        return currentFaculty()
                .map(Faculty::getFacultyId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No faculty profile is linked to this account"));
    }

    /** Convenience for services that already hold a course entity. */
    public void requireCourseAccess(Course course) {
        requireCourseAccess(course.getCourseId());
    }
}
