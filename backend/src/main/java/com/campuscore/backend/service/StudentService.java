package com.campuscore.backend.service;

import com.campuscore.backend.dto.ResourceDependencySummary;
import com.campuscore.backend.dto.StudentRequest;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.EnrollmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AccessScope accessScope;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            EnrollmentRepository enrollmentRepository) {

        this(studentRepository, userRepository, departmentRepository, enrollmentRepository, null);
    }

    @Autowired
    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            EnrollmentRepository enrollmentRepository,
            AccessScope accessScope) {

        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.accessScope = accessScope;
    }

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this(studentRepository, userRepository, departmentRepository, null, null);
    }

    // Get all students
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Students the acting caller may see, one page at a time.
     *
     * <p>Administrators see the whole directory. A faculty member only ever
     * sees the students enrolled in one of their currently assigned courses.
     *
     * <p>The scope is applied by the database query, not by filtering the
     * institution's students afterwards, and the reported total is counted over
     * the same permitted set. A page therefore cannot be used to discover how
     * many students exist elsewhere in the institution.
     *
     * <p>A faculty member without a current assignment gets an empty page,
     * which is the same answer the unpaged listing gives them.
     */
    public Page<Student> getStudentsPage(Pageable pageable) {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        if (accessScope.isAdmin()) {
            return studentRepository.findAll(pageable);
        }

        List<Long> courseIds = accessScope.assignedCourseIds();

        if (courseIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return studentRepository.findDistinctByCourseCourseIdIn(courseIds, pageable);
    }

    /**
     * Students the acting faculty member actually teaches.
     *
     * <p>The faculty directory is deliberately not a global student browser: a
     * faculty member only ever sees students enrolled in one of their currently
     * assigned courses.
     */
    public List<Student> getStudentsForAssignedCourses() {
        if (accessScope == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access scope is unavailable");
        }

        List<Long> courseIds = accessScope.assignedCourseIds();

        if (courseIds.isEmpty()) {
            return List.of();
        }

        return enrollmentRepository.findDistinctStudentsByCourseCourseIdIn(courseIds);
    }

    // Get student by ID
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }

    public Student getStudentForUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student profile is not linked to this account"
                ));

        return studentRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student profile is not linked to this account"
                ));
    }

    // Create student
    public Student createStudent(StudentRequest request) {

        if (studentRepository.existsByEnrollmentNumber(
                request.getEnrollmentNumber())) {

            throw new ResourceConflictException(
                    "Enrollment number already exists: " + request.getEnrollmentNumber()
            );
        }

        if (studentRepository.existsByUserUserId(
                request.getUserId())) {

            throw new ResourceConflictException(
                    "This user is already linked to a student account"
            );
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        Department department =
                departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        if (user.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException(
                    "User role must be STUDENT to link a student profile"
            );
        }

        Student student = new Student();

        student.setUser(user);
        student.setDepartment(department);
        student.setEnrollmentNumber(
                request.getEnrollmentNumber()
        );
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setPhone(request.getPhone());
        student.setAdmissionYear(request.getAdmissionYear());
        student.setStatus(Student.Status.ACTIVE);

        return studentRepository.save(student);
    }

    // Update student
    public Student updateStudent(
            Long id,
            StudentRequest request) {

        Student existingStudent = getStudentById(id);

        if (!existingStudent.getEnrollmentNumber().equals(request.getEnrollmentNumber())
                && studentRepository.existsByEnrollmentNumber(
                        request.getEnrollmentNumber())) {

            throw new ResourceConflictException(
                    "Enrollment number already exists: " + request.getEnrollmentNumber()
            );
        }

        if (!existingStudent.getUser().getUserId().equals(request.getUserId())
                && studentRepository.existsByUserUserId(
                        request.getUserId())) {

            throw new ResourceConflictException(
                    "This user is already linked to a student account"
            );
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        Department department =
                departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));

        existingStudent.setUser(user);
        existingStudent.setDepartment(department);
        existingStudent.setEnrollmentNumber(
                request.getEnrollmentNumber()
        );
        existingStudent.setFirstName(request.getFirstName());
        existingStudent.setLastName(request.getLastName());
        existingStudent.setDateOfBirth(request.getDateOfBirth());
        existingStudent.setPhone(request.getPhone());
        existingStudent.setAdmissionYear(request.getAdmissionYear());

        return studentRepository.save(existingStudent);
    }

    // Get dependency summary
    public ResourceDependencySummary getStudentDependencies(Long id) {
        Student student = getStudentById(id);
        long enrollments = enrollmentRepository != null ? enrollmentRepository.countByStudentStudentId(id) : 0;

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("enrollments", enrollments);

        boolean hasDependencies = (enrollments > 0);
        boolean canDelete = !hasDependencies;
        boolean canArchive = true; // Student has status ACTIVE, INACTIVE, GRADUATED

        String message = canDelete
                ? "Student has no academic enrollments and can be safely deleted."
                : String.format("Student has %d academic enrollment(s). Deactivate student status instead of deleting.", enrollments);

        return new ResourceDependencySummary("STUDENT", id, canDelete, canArchive, counts, message);
    }

    // Deactivate student
    public Student deactivateStudent(Long id) {
        Student student = getStudentById(id);
        student.setStatus(Student.Status.INACTIVE);
        return studentRepository.save(student);
    }

    // Delete student
    public void deleteStudent(Long id) {
        Student student = getStudentById(id);
        requireVerifiableDependencies();
        ResourceDependencySummary deps = getStudentDependencies(id);
        if (!deps.isCanDelete()) {
            throw new ResourceConflictException(deps.getMessage());
        }
        studentRepository.delete(student);
    }

    /**
     * Fail closed. Enrollments carry grades and attendance, so an absent
     * enrollment repository must never be mistaken for "this student has no
     * academic history".
     */
    private void requireVerifiableDependencies() {
        if (enrollmentRepository == null) {

            throw new ResourceConflictException(
                    "Academic enrollments cannot be verified, so this student cannot be deleted. "
                            + "Deactivate the student instead to preserve academic history.");
        }
    }
}
