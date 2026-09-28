package com.campuscore.backend.service;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.MarkRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MarkService {

    private final MarkRepository markRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;

    public MarkService(
            MarkRepository markRepository,
            EnrollmentRepository enrollmentRepository,
            AssessmentRepository assessmentRepository) {

        this.markRepository = markRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
    }

    public List<Mark> getAllMarks() {
        return markRepository.findAll();
    }

    public Mark getMarkById(Long id) {
        return markRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mark not found"));
    }

    public List<Mark> getMarksByEnrollment(Long enrollmentId) {
        return markRepository.findByEnrollmentEnrollmentId(enrollmentId);
    }

    public List<Mark> getMarksByAssessment(Long assessmentId) {
        return markRepository.findByAssessmentAssessmentId(assessmentId);
    }

    public Mark createMark(
            Long enrollmentId,
            Long assessmentId,
            BigDecimal marksObtained) {

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

        return markRepository.save(mark);
    }
}