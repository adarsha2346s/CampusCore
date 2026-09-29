package com.campuscore.backend.controller;

import com.campuscore.backend.dto.CourseRequest;
import com.campuscore.backend.dto.CourseResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.Course;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.service.CourseService;
import com.campuscore.backend.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final DepartmentService departmentService;

    public CourseController(
            CourseService courseService,
            DepartmentService departmentService) {

        this.courseService = courseService;
        this.departmentService = departmentService;
    }

    // GET all courses
    @GetMapping
    public ResponseEntity<?> getAllCourses(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "courseId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    courseService.getCoursesPage(pageable), this::toResponse));
        }

        List<CourseResponse> response =
                courseService.getAllCourses()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // GET course by ID
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                toResponse(
                        courseService.getCourseById(id)
                )
        );
    }

    // CREATE course
    @PostMapping
    public ResponseEntity<?> createCourse(
            @Valid @RequestBody CourseRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {

            Map<String, String> errors = new HashMap<>();

            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

            return ResponseEntity
                    .badRequest()
                    .body(errors);
        }

        Department department =
                departmentService.getDepartmentById(
                        request.getDepartmentId()
                );

        Course course = new Course();

        course.setDepartment(department);
        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setCredits(request.getCredits());
        course.setCapacity(request.getCapacity());

        Course savedCourse =
                courseService.createCourse(course);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(savedCourse));
    }

    // UPDATE course
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {

            Map<String, String> errors = new HashMap<>();

            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(
                            error.getField(),
                            error.getDefaultMessage()
                    )
            );

            return ResponseEntity
                    .badRequest()
                    .body(errors);
        }

        Department department =
                departmentService.getDepartmentById(
                        request.getDepartmentId()
                );

        Course course = new Course();

        course.setDepartment(department);
        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setCredits(request.getCredits());
        course.setCapacity(request.getCapacity());

        Course updatedCourse =
                courseService.updateCourse(id, course);

        return ResponseEntity.ok(
                toResponse(updatedCourse)
        );
    }

    // DELETE course
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id) {

        courseService.deleteCourse(id);

        return ResponseEntity.noContent().build();
    }

    private CourseResponse toResponse(Course course) {

        return new CourseResponse(
                course.getCourseId(),
                course.getDepartment().getDepartmentId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getCredits(),
                course.getCapacity(),
                String.valueOf(course.getStatus())
        );
    }
}
