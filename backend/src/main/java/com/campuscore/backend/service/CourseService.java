package com.campuscore.backend.service;

import com.campuscore.backend.entity.Course;
import com.campuscore.backend.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
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
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    // Create course
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    // Update course
    public Course updateCourse(Long id, Course courseDetails) {

        Course existingCourse = getCourseById(id);

        existingCourse.setCourseCode(courseDetails.getCourseCode());
        existingCourse.setCourseName(courseDetails.getCourseName());
        existingCourse.setCredits(courseDetails.getCredits());
        existingCourse.setCapacity(courseDetails.getCapacity());
        existingCourse.setStatus(courseDetails.getStatus());
        existingCourse.setDepartment(courseDetails.getDepartment());

        return courseRepository.save(existingCourse);
    }

    // Delete course
    public void deleteCourse(Long id) {

        Course course = getCourseById(id);

        courseRepository.delete(course);
    }
}
