package com.campuscore.backend.service;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.dto.MarkRequest;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.MarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MarkService {

    private final MarkRepository markRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AccessScope accessScope;

    private final AuditLogService auditLogService;

    public MarkService(
            MarkRepository markRepository,
            EnrollmentRepository enrollmentRepository,
            AssessmentRepository assessmentRepository,
            AuditLogService auditLogService) {

        this(markRepository, enrollmentRepository, assessmentRepository, null, auditLogService);
    }

        @Autowired
    public MarkService(
            MarkRepository markRepository,
            EnrollmentRepository enrollmentRepository,
            AssessmentRepository assessmentRepository,
            AccessScope accessScope,
            AuditLogService auditLogService) {

        this.markRepository = markRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.accessScope = accessScope;
        this.auditLogService = auditLogService;
    }

    public List<Mark> getAllMarks() {
        requireAdminForGlobalListing();
        return markRepository.findAll();
    }

    public Page<Mark> getMarksPage(Pageable pageable) {
        requireAdminForGlobalListing();
        return markRepository.findAll(pageable);
    }

    public Mark getMarkById(Long id) {
        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mark not found"));
        requireEnrollmentAccess(mark.getEnrollment().getEnrollmentId());
        return mark;
    }

    public List<Mark> getMarksByEnrollment(Long enrollmentId) {
        requireEnrollmentAccess(enrollmentId);
        return markRepository.findByEnrollmentEnrollmentId(enrollmentId);
    }

    public Page<Mark> getMarksByEnrollmentPage(Long enrollmentId, Pageable pageable) {
        requireEnrollmentAccess(enrollmentId);
        return markRepository.findByEnrollmentEnrollmentId(enrollmentId, pageable);
    }

    public List<Mark> getMarksByAssessment(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        requireCourseAccess(assessment.getCourse().getCourseId());
        return markRepository.findByAssessmentAssessmentId(assessmentId);
    }

    public Page<Mark> getMarksByAssessmentPage(Long assessmentId, Pageable pageable) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        requireCourseAccess(assessment.getCourse().getCourseId());
        return markRepository.findByAssessmentAssessmentId(assessmentId, pageable);
    }

    /**
     * Correct a recorded mark.
     *
     * <p>Only the awarded value changes. The enrollment and assessment stay put:
     * re-pointing a grade would either collide with the one-mark-per-enrollment-
     * per-assessment constraint or move a score onto the wrong assessment.
     *
     * <p>The value is re-validated against the assessment's current maximum, the
     * same rule create applies. That matters here because the maximum is editable
     * until the first mark exists, so a mark can be created and then have its
     * ceiling lowered underneath it.
     *
     * <p>enteredAt is deliberately left alone: it records when the mark was
     * entered, and correcting the value is not a re-entry.
     */
    @Transactional
    public Mark updateMark(Long id, MarkRequest request) {
        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mark not found"));

        requireEnrollmentAccess(mark.getEnrollment().getEnrollmentId());

        BigDecimal marksObtained = request.getMarksObtained();

        if (marksObtained.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResourceConflictException("Marks cannot be negative");
        }

        BigDecimal maxMarks = mark.getAssessment().getMaxMarks();

        if (marksObtained.compareTo(maxMarks) > 0) {
            throw new ResourceConflictException(
                    "Marks cannot be greater than maximum marks");
        }

        mark.setMarksObtained(marksObtained);

        Mark saved = markRepository.save(mark);

        auditLogService.record(
                "UPDATE_MARK",
                "Mark",
                saved.getMarkId(),
                "Corrected the mark for enrollment "
                        + saved.getEnrollment().getEnrollmentId() + " to "
                        + marksObtained
        );

        return saved;
    }

    /**
     * Delete a recorded mark.
     *
     * <p>A mark is a leaf: nothing in the schema references mark_id, and the
     * enrollment holds no stored aggregate that would drift out of step with the
     * remaining rows. Attendance is recorded against the enrollment, not the
     * mark, so removing one result leaves attendance untouched. Deletion is
     * therefore allowed, with the enrollment scope re-checked so the route's
     * role rule is not the only thing protecting the row.
     */
    @Transactional
    public void deleteMark(Long id) {
        Mark mark = markRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mark not found"));

        requireEnrollmentAccess(mark.getEnrollment().getEnrollmentId());

        markRepository.delete(mark);

        auditLogService.record(
                "DELETE_MARK",
                "Mark",
                mark.getMarkId(),
                "Deleted the mark for enrollment "
                        + mark.getEnrollment().getEnrollmentId()
        );
    }

    /**
     * Every mark belonging to one student, for the signed-in student's own view.
     *
     * <p>Ownership is re-checked here rather than trusting the controller: the
     * controller derives the id from the verified session, and this guard makes
     * sure the service cannot be reached with someone else's id. Fails closed when
     * the scope service is absent.
     */
    public List<Mark> getMarksForStudent(Long studentId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        accessScope.requireStudentAccess(studentId);

        return markRepository.findAllForStudent(studentId);
    }

    /**
     * Marks are owned by an enrollment, so enrollment scope decides visibility:
     * ADMIN, the faculty member assigned to that enrollment's course, or the
     * student who owns it. Fails closed when the scope service is not wired.
     */
    private void requireEnrollmentAccess(Long enrollmentId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        accessScope.requireEnrollmentAccess(enrollmentId);
    }

    private void requireCourseAccess(Long courseId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        accessScope.requireCourseAccess(courseId);
    }

    @Transactional
    public Mark createMark(
            Long enrollmentId,
            Long assessmentId,
            BigDecimal marksObtained) {

        requireEnrollmentAccess(enrollmentId);

        if (markRepository.existsByEnrollmentEnrollmentIdAndAssessmentAssessmentId(
                enrollmentId,
                assessmentId)) {

            throw new RuntimeException(
                    "Mark already exists for this enrollment and assessment"
            );
        }

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        if (marksObtained.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Marks cannot be negative");
        }

        if (marksObtained.compareTo(assessment.getMaxMarks()) > 0) {
            throw new RuntimeException(
                    "Marks cannot be greater than maximum marks"
            );
        }

        Mark mark = new Mark();

        mark.setEnrollment(enrollment);
        mark.setAssessment(assessment);
        mark.setMarksObtained(marksObtained);

        Mark saved = markRepository.save(mark);

        auditLogService.record(
                "CREATE_MARK",
                "Mark",
                saved.getMarkId(),
                "Recorded a mark of " + marksObtained + " for enrollment "
                        + enrollmentId + " against assessment " + assessmentId
        );

        return saved;
    }

    /**
     * Guards the institution-wide listings.
     *
     * These getters are not scope filtered, so they are reserved for
     * administrators. Requiring the role here as well as in the request matcher
     * means a future route that calls one of them cannot silently expose every
     * record in the institution to a faculty member or a student.
     */
    private void requireAdminForGlobalListing() {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        if (!accessScope.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This listing is restricted to administrators");
        }
    }

}
