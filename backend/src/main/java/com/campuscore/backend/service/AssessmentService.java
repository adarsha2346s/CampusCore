package com.campuscore.backend.service;

import com.campuscore.backend.entity.Assessment;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.repository.AssessmentRepository;
import com.campuscore.backend.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CourseRepository courseRepository;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            CourseRepository courseRepository) {

        this.assessmentRepository = assessmentRepository;
        this.courseRepository = courseRepository;
    }

    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
    }

    public List<Assessment> getAssessmentsByCourse(Long courseId) {
        return assessmentRepository.findByCourseCourseId(courseId);
    }

    public Assessment createAssessment(
            Long courseId,
            String name,
            Assessment.AssessmentType assessmentType,
            BigDecimal maxMarks,
            BigDecimal weight) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Assessment assessment = new Assessment();

        assessment.setCourse(course);
        assessment.setName(name);
        assessment.setAssessmentType(assessmentType);
        assessment.setMaxMarks(maxMarks);
        assessment.setWeight(weight);

        return assessmentRepository.save(assessment);
    }
}