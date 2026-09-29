package com.campuscore.backend.service;

import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public FacultyService(
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {

        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public Page<Faculty> getFacultyPage(Pageable pageable) {
        return facultyRepository.findAll(pageable);
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
    }

    public Faculty getFacultyForUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Faculty profile is not linked to this account"
                ));

        return facultyRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Faculty profile is not linked to this account"
                ));
    }

    public Faculty createFaculty(
            Long userId,
            Long departmentId,
            String employeeNumber,
            String firstName,
            String lastName,
            String phone) {

        if (facultyRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new RuntimeException("Employee number already exists");
        }

        if (facultyRepository.existsByUserUserId(userId)) {
            throw new RuntimeException(
                    "This user is already linked to a faculty"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        if (user.getRole() != User.Role.FACULTY) {
            throw new RuntimeException("User role must be FACULTY");
        }

        Faculty faculty = new Faculty();

        faculty.setUser(user);
        faculty.setDepartment(department);
        faculty.setEmployeeNumber(employeeNumber);
        faculty.setFirstName(firstName);
        faculty.setLastName(lastName);
        faculty.setPhone(phone);
        faculty.setStatus(Faculty.Status.ACTIVE);

        return facultyRepository.save(faculty);
    }
}
