package com.campuscore.backend.service;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Enrollment;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.StudentRepository;
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

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository) {

        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public Page<Enrollment> getEnrollmentsPage(Pageable pageable) {
        return enrollmentRepository.findAll(pageable);
    }

    public List<Enrollment> getEnrollmentsForStudent(Long studentId) {
        return enrollmentRepository.findByStudentStudentId(studentId);
    }

    public Enrollment getEnrollmentById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
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
