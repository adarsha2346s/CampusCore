package com.campuscore.backend.service;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.dto.AssessmentRequest;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.MarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CourseRepository courseRepository;
    private final AccessScope accessScope;

    private final MarkRepository markRepository;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            CourseRepository courseRepository) {

        this(assessmentRepository, courseRepository, null, null);
    }

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            CourseRepository courseRepository,
            AccessScope accessScope) {

        this(assessmentRepository, courseRepository, accessScope, null);
    }

        @Autowired
    public AssessmentService(
            AssessmentRepository assessmentRepository,
            CourseRepository courseRepository,
            AccessScope accessScope,
            MarkRepository markRepository) {

        this.assessmentRepository = assessmentRepository;
        this.courseRepository = courseRepository;
        this.accessScope = accessScope;
        this.markRepository = markRepository;
    }

    /**
     * Assessment metadata for a student's own marks.
     *
     * <p>Ownership is re-checked here rather than trusting the caller: the
     * controller derives the id from the verified session, and this guard makes
     * sure the service cannot be reached with someone else's id. Fails closed
     * when the scope service is absent.
     */
    public List<Assessment> getAssessmentsForStudent(Long studentId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        accessScope.requireStudentAccess(studentId);

        return assessmentRepository.findDistinctForStudent(studentId);
    }

    public List<Assessment> getAllAssessments() {
        requireAdminForGlobalListing();
        return assessmentRepository.findAll();
    }

    public Page<Assessment> getAssessmentsPage(Pageable pageable) {
        requireAdminForGlobalListing();
        return assessmentRepository.findAll(pageable);
    }

    public Assessment getAssessmentById(Long id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        requireCourseAccess(assessment.getCourse().getCourseId());
        return assessment;
    }

    public List<Assessment> getAssessmentsByCourse(Long courseId) {
        requireCourseAccess(courseId);
        return assessmentRepository.findByCourseCourseId(courseId);
    }

    public Page<Assessment> getAssessmentsByCoursePage(Long courseId, Pageable pageable) {
        requireCourseAccess(courseId);
        return assessmentRepository.findByCourseCourseId(courseId, pageable);
    }

    public Assessment createAssessment(
            Long courseId,
            String name,
            Assessment.AssessmentType assessmentType,
            BigDecimal maxMarks,
            BigDecimal weight) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        requireCourseAccess(courseId);

        Assessment assessment = new Assessment();

        assessment.setCourse(course);
        assessment.setName(name);
        assessment.setAssessmentType(assessmentType);
        assessment.setMaxMarks(maxMarks);
        assessment.setWeight(weight);

        return assessmentRepository.save(assessment);
    }

    /**
     * Update an assessment's own details.
     *
     * <p>Maximum marks is locked once marks exist. Rescaling it would silently
     * reinterpret every grade already recorded against the assessment, turning a
     * 40 out of 100 into a 40 out of 50 without touching a single mark row. Name,
     * type, weight and date stay editable because they describe the assessment
     * rather than rescale its results.
     *
     * <p>The course is intentionally not editable: it is the owning offering, and
     * moving an assessment between courses would strand its marks.
     */
    public Assessment updateAssessment(Long id, AssessmentRequest request) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment not found"));

        boolean rescaling = assessment.getMaxMarks().compareTo(request.getMaxMarks()) != 0;

        if (rescaling && dependentMarkCount(id) > 0) {
            throw new ResourceConflictException(
                    "Maximum marks cannot be changed once marks reference this assessment");
        }

        assessment.setName(request.getName());
        assessment.setAssessmentType(request.getAssessmentType());
        assessment.setMaxMarks(request.getMaxMarks());
        assessment.setWeight(request.getWeight());
        assessment.setAssessmentDate(request.getAssessmentDate());

        return assessmentRepository.save(assessment);
    }

    /**
     * Delete an assessment that no marks reference.
     *
     * <p>An assessment is the parent of the mark rows: the database foreign key is
     * ON DELETE RESTRICT, so deleting a graded assessment would raise a constraint
     * violation. Blocking it here with a message about the recorded marks is
     * clearer than surfacing that as a server error. Assessments carry no status
     * column, so there is no archive state to fall back on.
     */
    public void deleteAssessment(Long id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment not found"));

        if (dependentMarkCount(id) > 0) {
            throw new ResourceConflictException(
                    "Assessment cannot be deleted while marks reference it");
        }

        assessmentRepository.delete(assessment);
    }

    /**
     * Number of marks that depend on an assessment.
     *
     * <p>Fails closed: if the repository is not wired the dependency cannot be
     * checked, so the guard must refuse rather than assume it is safe to delete or
     * rescale.
     */
    private long dependentMarkCount(Long assessmentId) {
        if (markRepository == null) {
            throw new ResourceConflictException(
                    "Mark dependencies cannot be verified, so this assessment was left unchanged");
        }
        return Optional.ofNullable(markRepository.countByAssessmentAssessmentId(assessmentId)).orElse(0L);
    }

    /**
     * Assessments belong to a course, so course scope decides visibility. Fails
     * closed when the scope service is not wired.
     */
    private void requireCourseAccess(Long courseId) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }
        accessScope.requireCourseAccess(courseId);
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
