package com.campuscore.backend.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AccessScope accessScope;
    private final MarkRepository markRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AccessScope accessScope,
            MarkRepository markRepository,
            AttendanceRecordRepository attendanceRecordRepository) {

        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.accessScope = accessScope;
        this.markRepository = markRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    /** Convenience constructor for callers that only need the read and create paths. */
    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AccessScope accessScope) {

        this(enrollmentRepository, studentRepository, courseRepository, accessScope, null, null);
    }

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository) {

        this(enrollmentRepository, studentRepository, courseRepository, null, null, null);
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public Page<Enrollment> getEnrollmentsPage(Pageable pageable) {
        return enrollmentRepository.findAll(pageable);
    }

    /**
     * A student lists only their own enrollments; faculty list the enrollments of
     * students they actually teach.
     */
    /**
     * Enrollments of a single course.
     *
     * <p>Course scope decides visibility: an administrator sees any course, a
     * faculty member only a course they are currently assigned to. Fails closed
     * when the scope service is absent.
     */
    public List<Enrollment> getEnrollmentsByCourse(Long courseId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        accessScope.requireCourseAccess(courseId);

        return enrollmentRepository.findByCourseCourseId(courseId);
    }

    public List<Enrollment> getEnrollmentsForStudent(Long studentId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        accessScope.requireStudentAccess(studentId);
        return enrollmentRepository.findByStudentStudentId(studentId);
    }

    public Enrollment getEnrollmentById(Long id) {
        if (accessScope != null) {
            accessScope.requireEnrollmentAccess(id);
        }
        return getEnrollmentByIdInternal(id);
    }

    private Enrollment getEnrollmentByIdInternal(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
    }

    /**
     * Academic records that would be orphaned or silently reassigned by removing
     * or re-terms an enrollment.
     */
    private long dependentRecordCount(Long enrollmentId) {
        if (markRepository == null || attendanceRecordRepository == null) {
            // Fail closed: unverifiable dependents must be treated as present.
            return Long.MAX_VALUE;
        }
        return markRepository.countByEnrollmentEnrollmentId(enrollmentId)
                + attendanceRecordRepository.countByEnrollmentEnrollmentId(enrollmentId);
    }

    /**
     * Change an enrollment's term or status.
     *
     * <p>Status is the intended lifecycle control and is always editable. The term
     * is only editable while no marks or attendance exist, because re-terms an
     * enrollment that already has graded work would silently move those results
     * into a different term.
     */
    public Enrollment updateEnrollment(
            Long enrollmentId,
            String semester,
            String academicYear,
            Enrollment.Status status) {

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found with ID: " + enrollmentId));

        boolean termChanged =
                !enrollment.getSemester().equals(semester)
                        || !enrollment.getAcademicYear().equals(academicYear);

        if (termChanged && dependentRecordCount(enrollmentId) > 0) {
            throw new ResourceConflictException(
                    "Enrollment already has marks or attendance records, so its term cannot be "
                            + "changed. Change the status instead to record the outcome.");
        }

        if (termChanged && enrollmentRepository
                .existsByStudentStudentIdAndCourseCourseIdAndSemesterAndAcademicYear(
                        enrollment.getStudent().getStudentId(),
                        enrollment.getCourse().getCourseId(),
                        semester,
                        academicYear)) {

            throw new ResourceConflictException(
                    "This student is already enrolled in the same course for "
                            + semester + " " + academicYear);
        }

        enrollment.setSemester(semester);
        enrollment.setAcademicYear(academicYear);
        enrollment.setStatus(status);

        return enrollmentRepository.save(enrollment);
    }

    /**
     * Remove an enrollment that carries no academic records.
     *
     * <p>An enrollment with marks or attendance is never deleted: those rows would
     * be orphaned and the academic history lost. Recording the outcome through the
     * status is the safe alternative.
     */
    public void deleteEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found with ID: " + enrollmentId));

        long marks = markRepository == null
                ? Long.MAX_VALUE
                : markRepository.countByEnrollmentEnrollmentId(enrollmentId);

        long attendance = attendanceRecordRepository == null
                ? Long.MAX_VALUE
                : attendanceRecordRepository.countByEnrollmentEnrollmentId(enrollmentId);

        if (marks > 0 || attendance > 0) {
            throw new ResourceConflictException(
                    "Enrollment has " + marks + " mark(s) and " + attendance
                            + " attendance record(s) and cannot be deleted. "
                            + "Change the status instead to record the outcome.");
        }

        enrollmentRepository.delete(enrollment);
    }

    public Enrollment createEnrollment(
            Long studentId,
            Long courseId,
            String semester,
            String academicYear) {

        if (enrollmentRepository
                .existsByStudentStudentIdAndCourseCourseIdAndSemesterAndAcademicYear(
                        studentId,
                        courseId,
                        semester,
                        academicYear)) {

            throw new RuntimeException(
                    "Student is already enrolled in this course for this semester"
            );
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrollment enrollment = new Enrollment();

        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setSemester(semester);
        enrollment.setAcademicYear(academicYear);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus(Enrollment.Status.ENROLLED);

        return enrollmentRepository.save(enrollment);
    }
}
