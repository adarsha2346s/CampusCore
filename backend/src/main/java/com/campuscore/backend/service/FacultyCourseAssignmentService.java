package com.campuscore.backend.service;

import com.campuscore.backend.dto.FacultyAssignmentResponse;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.FacultyCourseAssignment;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Faculty-to-course assignment rules.
 *
 * <p>Design decisions, kept deliberately small:
 * <ul>
 *   <li>Assignments are append-only history. Reassignment closes the previous
 *       row instead of deleting or overwriting it, so who taught a course before
 *       is never lost.</li>
 *   <li>Duplicate <em>active</em> assignment of the same faculty to the same
 *       course is rejected. A closed assignment for the same pair is allowed, so
 *       a faculty member can return to a course later.</li>
 *   <li>Nothing here is back-dated or invented. Pre-existing V1 courses simply
 *       have no assignment until an administrator creates one.</li>
 * </ul>
 */
@Service
public class FacultyCourseAssignmentService {

    private final FacultyCourseAssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public FacultyCourseAssignmentService(
            FacultyCourseAssignmentRepository assignmentRepository,
            CourseRepository courseRepository,
            FacultyRepository facultyRepository,
            EnrollmentRepository enrollmentRepository,
            AuditLogService auditLogService) {

        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditLogService = auditLogService;
    }

    public FacultyCourseAssignmentService(
            FacultyCourseAssignmentRepository assignmentRepository,
            AuditLogService auditLogService) {

        this(assignmentRepository, null, null, null, auditLogService);
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    /** Current (open) assignments for a course. */
    public List<FacultyAssignmentResponse> getActiveAssignments(Long courseId) {
        requireCourse(courseId);

        return assignmentRepository
                .findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(courseId)
                .stream()
                .map(FacultyCourseAssignmentService::toResponse)
                .toList();
    }

    /** Full assignment history for a course, including closed assignments. */
    public List<FacultyAssignmentResponse> getAssignmentHistory(Long courseId) {
        requireCourse(courseId);

        return assignmentRepository
                .findByCourseCourseIdOrderByAssignedAtDesc(courseId)
                .stream()
                .map(FacultyCourseAssignmentService::toResponse)
                .toList();
    }

    /** Current courses taught by a faculty member. */
    public List<FacultyAssignmentResponse> getActiveAssignmentsForFaculty(Long facultyId) {
        requireFaculty(facultyId);

        return assignmentRepository
                .findByFacultyFacultyIdAndEndedAtIsNullOrderByAssignedAtDesc(facultyId)
                .stream()
                .map(FacultyCourseAssignmentService::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Writes
    // ------------------------------------------------------------------

    @Transactional
    public FacultyAssignmentResponse assignFaculty(Long courseId, Long facultyId) {
        Course course = requireCourse(courseId);
        Faculty faculty = requireAssignableFaculty(facultyId);

        if (assignmentRepository
                .findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(courseId, facultyId)
                .isPresent()) {

            throw new ResourceConflictException(
                    "Faculty member " + faculty.getEmployeeNumber()
                            + " is already actively assigned to course "
                            + course.getCourseCode());
        }

        FacultyCourseAssignment assignment = new FacultyCourseAssignment();
        assignment.setCourse(course);
        assignment.setFaculty(faculty);
        assignment.setAssignedAt(LocalDateTime.now());

        FacultyAssignmentResponse saved =
                toResponse(assignmentRepository.save(assignment));

        auditLogService.record(
                "ASSIGN_FACULTY",
                "FacultyCourseAssignment",
                saved.getAssignmentId(),
                "Assigned faculty " + faculty.getEmployeeNumber() + " to course "
                        + course.getCourseCode()
        );

        return saved;
    }

    /**
     * Hands the course over to a different faculty member. Previous open rows are
     * closed rather than deleted, which preserves the teaching history and leaves
     * enrollments, assessments, marks and attendance untouched.
     */
    @Transactional
    public FacultyAssignmentResponse reassignFaculty(Long courseId, Long facultyId) {
        Course course = requireCourse(courseId);
        Faculty faculty = requireAssignableFaculty(facultyId);

        List<FacultyCourseAssignment> current =
                assignmentRepository
                        .findByCourseCourseIdAndEndedAtIsNullOrderByAssignedAtDesc(courseId);

        boolean alreadyAssigned = current.stream()
                .anyMatch(assignment ->
                        assignment.getFaculty().getFacultyId().equals(facultyId));

        if (alreadyAssigned) {
            throw new ResourceConflictException(
                    "Faculty member " + faculty.getEmployeeNumber()
                            + " is already actively assigned to course "
                            + course.getCourseCode());
        }

        LocalDateTime now = LocalDateTime.now();
        if (!current.isEmpty()) {
            for (FacultyCourseAssignment assignment : current) {
                assignment.setEndedAt(now);
            }
            assignmentRepository.saveAll(current);
        }

        FacultyCourseAssignment replacement = new FacultyCourseAssignment();
        replacement.setCourse(course);
        replacement.setFaculty(faculty);
        replacement.setAssignedAt(now);

        FacultyAssignmentResponse saved =
                toResponse(assignmentRepository.save(replacement));

        // One handover is one event: the assignments it closed and the one it
        // opened are the same administrative action.
        auditLogService.record(
                "REASSIGN_FACULTY",
                "FacultyCourseAssignment",
                saved.getAssignmentId(),
                "Reassigned course " + course.getCourseCode() + " to faculty "
                        + faculty.getEmployeeNumber()
                        + (current.isEmpty()
                                ? ""
                                : ", closing " + current.size() + " previous assignment(s)")
        );

        return saved;
    }

    /**
     * Ends an assignment. Rows are never hard-deleted so the history survives.
     * Removing the last assignment of a course that already has enrolments is
     * refused, because that would leave existing student records without an
     * assigned faculty member; reassign instead.
     */
    @Transactional
    public void removeAssignment(Long courseId, Long facultyId) {
        Course course = requireCourse(courseId);
        FacultyCourseAssignment assignment =
                assignmentRepository
                        .findByCourseCourseIdAndFacultyFacultyIdAndEndedAtIsNull(courseId, facultyId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No active assignment of faculty " + facultyId
                                        + " to course " + course.getCourseCode()));

        long activeAssignments = assignmentRepository
                .countByCourseCourseIdAndEndedAtIsNull(courseId);

        if (activeAssignments <= 1 && hasAcademicHistory(course)) {
            throw new ResourceConflictException(
                    "Course " + course.getCourseCode()
                            + " still has enrolled students. Reassign the course to another "
                            + "faculty member instead of removing the last assignment.");
        }

        assignment.setEndedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        auditLogService.record(
                "END_FACULTY_ASSIGNMENT",
                "FacultyCourseAssignment",
                assignment.getAssignmentId(),
                "Ended the assignment of faculty " + assignment.getFaculty().getEmployeeNumber()
                        + " to course " + assignment.getCourse().getCourseCode()
        );
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean hasAcademicHistory(Course course) {
        if (enrollmentRepository == null) {
            // Fail closed: unverifiable enrolments must not be treated as none.
            return true;
        }
        return enrollmentRepository.countByCourseCourseId(course.getCourseId()) > 0;
    }

    private Course requireCourse(Long courseId) {
        if (courseRepository == null) {
            throw new ResourceNotFoundException("Course not found with ID: " + courseId);
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course not found with ID: " + courseId));
    }

    private Faculty requireFaculty(Long facultyId) {
        if (facultyRepository == null) {
            throw new ResourceNotFoundException("Faculty not found with ID: " + facultyId);
        }
        return facultyRepository.findById(facultyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Faculty not found with ID: " + facultyId));
    }

    private Faculty requireAssignableFaculty(Long facultyId) {
        Faculty faculty = requireFaculty(facultyId);

        if (faculty.getStatus() != Faculty.Status.ACTIVE) {
            throw new ResourceConflictException(
                    "Faculty member " + faculty.getEmployeeNumber()
                            + " is INACTIVE and cannot be assigned to a course");
        }

        return faculty;
    }

    static FacultyAssignmentResponse toResponse(FacultyCourseAssignment assignment) {
        Course course = assignment.getCourse();
        Faculty faculty = assignment.getFaculty();

        String facultyName = faculty.getFirstName()
                + (faculty.getLastName() == null ? "" : " " + faculty.getLastName());

        return new FacultyAssignmentResponse(
                assignment.getAssignmentId(),
                course.getCourseId(),
                course.getCourseCode(),
                course.getCourseName(),
                faculty.getFacultyId(),
                faculty.getEmployeeNumber(),
                facultyName,
                assignment.getAssignedAt(),
                assignment.getEndedAt(),
                assignment.isActive());
    }
}
