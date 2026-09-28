package com.campuscore.backend.service;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Mark;
import com.campuscore.backend.entity.GradingPolicy;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.MarkRepository;
import com.campuscore.backend.repository.GradingPolicyRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class GpaCalculationService {

    private final GradingPolicyRepository gradingPolicyRepository;
    private final AssessmentRepository assessmentRepository;
    private final MarkRepository markRepository;
    private final EnrollmentRepository enrollmentRepository;

    public GpaCalculationService(
            GradingPolicyRepository gradingPolicyRepository,
            AssessmentRepository assessmentRepository,
            MarkRepository markRepository,
            EnrollmentRepository enrollmentRepository) {

        this.gradingPolicyRepository = gradingPolicyRepository;
        this.assessmentRepository = assessmentRepository;
        this.markRepository = markRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public GradingPolicy getGradeForPercentage(
            BigDecimal percentage,
            String policyName) {

        return gradingPolicyRepository
                .findMatchingPolicy(policyName, percentage)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No grading policy found for percentage: "
                                        + percentage
                        )
                );
    }

    public BigDecimal calculateWeightedPercentage(
            BigDecimal marksObtained,
            BigDecimal maxMarks,
            BigDecimal weight) {

        if (maxMarks.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Maximum marks must be greater than zero"
            );
        }

        if (marksObtained.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(
                    "Marks obtained cannot be negative"
            );
        }

        if (marksObtained.compareTo(maxMarks) > 0) {
            throw new RuntimeException(
                    "Marks obtained cannot be greater than maximum marks"
            );
        }

        if (weight.compareTo(BigDecimal.ZERO) < 0
                || weight.compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new RuntimeException(
                    "Assessment weight must be between 0 and 100"
            );
        }

        BigDecimal percentage = marksObtained
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        maxMarks,
                        2,
                        RoundingMode.HALF_UP
                );

        return percentage
                .multiply(weight)
                .divide(
                        BigDecimal.valueOf(100),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    public CourseGpaResult calculateCourseGpa(
            Long enrollmentId,
            String policyName) {

        Enrollment enrollment = enrollmentRepository
                .findById(enrollmentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Enrollment not found: " + enrollmentId
                        )
                );

        Long courseId = enrollment.getCourse().getCourseId();

        List<Assessment> assessments =
                assessmentRepository.findByCourseCourseId(courseId);

        if (assessments.isEmpty()) {
            throw new RuntimeException(
                    "No assessments found for this course"
            );
        }

        BigDecimal totalWeightedPercentage = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Assessment assessment : assessments) {

            Mark mark = markRepository
                    .findByEnrollmentEnrollmentIdAndAssessmentAssessmentId(
                            enrollmentId,
                            assessment.getAssessmentId()
                    )
                    .orElse(null);

            if (mark == null) {
                continue;
            }

            BigDecimal weightedPercentage =
                    calculateWeightedPercentage(
                            mark.getMarksObtained(),
                            assessment.getMaxMarks(),
                            assessment.getWeight()
                    );

            totalWeightedPercentage =
                    totalWeightedPercentage.add(weightedPercentage);

            totalWeight =
                    totalWeight.add(assessment.getWeight());
        }

        boolean complete = totalWeight.compareTo(
                BigDecimal.valueOf(100)
        ) == 0;

        BigDecimal currentPercentage;

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {

            currentPercentage = BigDecimal.ZERO;

        } else {

            currentPercentage = totalWeightedPercentage
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            totalWeight,
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        GradingPolicy gradingPolicy =
                getGradeForPercentage(
                        currentPercentage,
                        policyName
                );

        return new CourseGpaResult(
                enrollment.getEnrollmentId(),
                courseId,
                enrollment.getCourse().getCourseCode(),
                enrollment.getCourse().getCourseName(),
                totalWeight,
                currentPercentage,
                gradingPolicy.getGrade(),
                gradingPolicy.getGradePoint(),
                complete
        );
    }

    public static class CourseGpaResult {

        private Long enrollmentId;
        private Long courseId;
        private String courseCode;
        private String courseName;

        private BigDecimal assessedWeight;
        private BigDecimal currentPercentage;

        private String grade;
        private BigDecimal gradePoint;

        private boolean complete;

        public CourseGpaResult(
                Long enrollmentId,
                Long courseId,
                String courseCode,
                String courseName,
                BigDecimal assessedWeight,
                BigDecimal currentPercentage,
                String grade,
                BigDecimal gradePoint,
                boolean complete) {

            this.enrollmentId = enrollmentId;
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.assessedWeight = assessedWeight;
            this.currentPercentage = currentPercentage;
            this.grade = grade;
            this.gradePoint = gradePoint;
            this.complete = complete;
        }

        public Long getEnrollmentId() {
            return enrollmentId;
        }

        public Long getCourseId() {
            return courseId;
        }

        public String getCourseCode() {
            return courseCode;
        }

        public String getCourseName() {
            return courseName;
        }

        public BigDecimal getAssessedWeight() {
            return assessedWeight;
        }

        public BigDecimal getCurrentPercentage() {
            return currentPercentage;
        }

        public String getGrade() {
            return grade;
        }

        public BigDecimal getGradePoint() {
            return gradePoint;
        }

        public boolean isComplete() {
            return complete;
        }
    }
}