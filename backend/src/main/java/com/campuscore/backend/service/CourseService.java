package com.campuscore.backend.service;

import com.campuscore.backend.dto.ResourceDependencySummary;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final FacultyCourseAssignmentRepository facultyCourseAssignmentRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssessmentRepository assessmentRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            FacultyCourseAssignmentRepository facultyCourseAssignmentRepository,
            AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.facultyCourseAssignmentRepository = facultyCourseAssignmentRepository;
        this.auditLogService = auditLogService;
    }

    public CourseService(
            CourseRepository courseRepository,
            AuditLogService auditLogService) {
        this(courseRepository, null, null, null, null, auditLogService);
    }

    // Get all courses
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Page<Course> getCoursesPage(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    // Get course by ID
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
    }

    // Create course
    @Transactional
    public Course createCourse(Course course) {
        Course saved = courseRepository.save(course);

        auditLogService.record(
                "CREATE_COURSE",
                "Course",
                saved.getCourseId(),
                "Created course " + saved.getCourseCode() + " (" + saved.getCourseName() + ")"
        );

        return saved;
    }

    // Update course
    @Transactional
    public Course updateCourse(Long id, Course courseDetails) {
        Course existingCourse = getCourseById(id);

        existingCourse.setCourseCode(courseDetails.getCourseCode());
        existingCourse.setCourseName(courseDetails.getCourseName());
        existingCourse.setCredits(courseDetails.getCredits());
        existingCourse.setCapacity(courseDetails.getCapacity());
        existingCourse.setDepartment(courseDetails.getDepartment());

        // Status is deliberately not copied from the incoming payload. The update
        // request carries no status field, so copying it would reset every edited
        // course back to ACTIVE and silently resurrect a deactivated one. Status
        // changes go through deactivateCourse only.

        Course saved = courseRepository.save(existingCourse);

        auditLogService.record(
                "UPDATE_COURSE",
                "Course",
                saved.getCourseId(),
                "Updated course " + saved.getCourseCode() + " (" + saved.getCourseName() + ")"
        );

        return saved;
    }

    // Get dependency summary
    public ResourceDependencySummary getCourseDependencies(Long id) {
        Course course = getCourseById(id);
        long enrollments = enrollmentRepository != null ? enrollmentRepository.countByCourseCourseId(id) : 0;
        long assessments = assessmentRepository != null ? assessmentRepository.countByCourseCourseId(id) : 0;
        long sessions = attendanceSessionRepository != null ? attendanceSessionRepository.countByCourseCourseId(id) : 0;
        long assignments = facultyCourseAssignmentRepository != null
                ? facultyCourseAssignmentRepository.countByCourseCourseId(id) : 0;

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("enrollments", enrollments);
        counts.put("assessments", assessments);
        counts.put("attendanceSessions", sessions);
        counts.put("facultyAssignments", assignments);

        boolean hasDependencies =
                (enrollments > 0 || assessments > 0 || sessions > 0 || assignments > 0);
        boolean canDelete = !hasDependencies;
        boolean canArchive = true; // Course has status ACTIVE/INACTIVE

        String message = canDelete
                ? "Course has no dependent records and can be safely deleted."
                : String.format("Course has %d enrollment(s), %d assessment(s), %d attendance session(s), and %d faculty assignment(s). Deactivate or archive it instead of deleting.",
                enrollments, assessments, sessions, assignments);

        return new ResourceDependencySummary("COURSE", id, canDelete, canArchive, counts, message);
    }

    // Safe deactivation
    @Transactional
    public Course deactivateCourse(Long id) {
        Course course = getCourseById(id);
        course.setStatus(Course.Status.INACTIVE);
        Course saved = courseRepository.save(course);

        auditLogService.record(
                "DEACTIVATE_COURSE",
                "Course",
                saved.getCourseId(),
                "Deactivated course " + saved.getCourseCode()
        );

        return saved;
    }

    // Delete course
    @Transactional
    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        requireVerifiableDependencies();
        ResourceDependencySummary dependencies = getCourseDependencies(id);
        if (!dependencies.isCanDelete()) {
            throw new ResourceConflictException(dependencies.getMessage());
        }
        courseRepository.delete(course);

        auditLogService.record(
                "DELETE_COURSE",
                "Course",
                course.getCourseId(),
                "Deleted course " + course.getCourseCode()
        );
    }

    /**
     * Fail closed. The reduced constructors exist for tests that never touch the
     * dependency lookups, and an absent repository must never be mistaken for
     * "this course has no dependents".
     */
    private void requireVerifiableDependencies() {
        if (enrollmentRepository == null
                || assessmentRepository == null
                || attendanceSessionRepository == null
                || facultyCourseAssignmentRepository == null) {

            throw new ResourceConflictException(
                    "Dependent records cannot be verified, so this course cannot be deleted. "
                            + "Deactivate the course instead to preserve academic history.");
        }
    }
}
