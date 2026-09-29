package com.campuscore.backend.service;

import com.campuscore.backend.dto.StudentRequest;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Student;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.StudentRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public StudentService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {

        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    // Get all students
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Page<Student> getStudentsPage(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    // Get student by ID
    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
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

            throw new RuntimeException(
                    "Enrollment number already exists"
            );
        }

        if (studentRepository.existsByUserUserId(
                request.getUserId())) {

            throw new RuntimeException(
                    "This user is already linked to a student"
            );
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Department department =
                departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() ->
                                new RuntimeException("Department not found"));

        if (user.getRole() != User.Role.STUDENT) {
            throw new RuntimeException(
                    "User role must be STUDENT"
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

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Department department =
                departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() ->
                                new RuntimeException("Department not found"));

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

    // Deactivate student
    public void deactivateStudent(Long id) {

        Student student = getStudentById(id);

        student.setStatus(Student.Status.INACTIVE);

        studentRepository.save(student);
    }
}
