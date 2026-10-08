package com.campuscore.backend.service;

import com.campuscore.backend.dto.ResourceDependencySummary;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.CourseRepository;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public DepartmentService(
            DepartmentRepository departmentRepository,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            CourseRepository courseRepository) {
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
    }

    public DepartmentService(DepartmentRepository departmentRepository) {
        this(departmentRepository, null, null, null);
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }

    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    public Department updateDepartment(Long id, Department departmentDetails) {
        Department existingDepartment = getDepartmentById(id);

        existingDepartment.setName(departmentDetails.getName());
        existingDepartment.setCode(departmentDetails.getCode());

        return departmentRepository.save(existingDepartment);
    }

    public ResourceDependencySummary getDepartmentDependencies(Long id) {
        Department department = getDepartmentById(id);
        long students = studentRepository != null ? studentRepository.countByDepartmentDepartmentId(id) : 0;
        long faculty = facultyRepository != null ? facultyRepository.countByDepartmentDepartmentId(id) : 0;
        long courses = courseRepository != null ? courseRepository.countByDepartmentDepartmentId(id) : 0;

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("students", students);
        counts.put("faculty", faculty);
        counts.put("courses", courses);

        boolean hasDependencies = (students > 0 || faculty > 0 || courses > 0);
        boolean canDelete = !hasDependencies;
        boolean canArchive = false; // Department schema does not currently define an active/status column

        String message = canDelete
                ? "Department has no associated students, faculty, or courses and can be safely deleted."
                : String.format("Department has %d student(s), %d faculty member(s), and %d course(s). Dependent records must be reassigned before deleting.",
                students, faculty, courses);

        return new ResourceDependencySummary("DEPARTMENT", id, canDelete, canArchive, counts, message);
    }

    public void deleteDepartment(Long id) {
        Department department = getDepartmentById(id);
        requireVerifiableDependencies();
        ResourceDependencySummary dependencies = getDepartmentDependencies(id);
        if (!dependencies.isCanDelete()) {
            throw new ResourceConflictException(dependencies.getMessage());
        }
        departmentRepository.delete(department);
    }

    /**
     * Fail closed. An absent dependency repository must never be mistaken for
     * "this department has no students, faculty, or courses".
     */
    private void requireVerifiableDependencies() {
        if (studentRepository == null
                || facultyRepository == null
                || courseRepository == null) {

            throw new ResourceConflictException(
                    "Dependent records cannot be verified, so this department cannot be deleted. "
                            + "Reassign or remove its students, faculty, and courses first.");
        }
    }
}