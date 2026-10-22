package com.campuscore.backend.service;

import com.campuscore.backend.dto.ResourceDependencySummary;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.exception.ResourceConflictException;
import com.campuscore.backend.exception.ResourceNotFoundException;
import com.campuscore.backend.repository.AttendanceSessionRepository;
import com.campuscore.backend.repository.DepartmentRepository;
import com.campuscore.backend.repository.FacultyCourseAssignmentRepository;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final FacultyCourseAssignmentRepository facultyCourseAssignmentRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public FacultyService(
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            FacultyCourseAssignmentRepository facultyCourseAssignmentRepository,
            AuditLogService auditLogService) {

        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.facultyCourseAssignmentRepository = facultyCourseAssignmentRepository;
        this.auditLogService = auditLogService;
    }

    public FacultyService(
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AuditLogService auditLogService) {
        this(facultyRepository, userRepository, departmentRepository, null, null, auditLogService);
    }

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public Page<Faculty> getFacultyPage(Pageable pageable) {
        return facultyRepository.findAll(pageable);
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with ID: " + id));
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

    @Transactional
    public Faculty createFaculty(
            Long userId,
            Long departmentId,
            String employeeNumber,
            String firstName,
            String lastName,
            String phone) {

        if (facultyRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new ResourceConflictException("Employee number already exists: " + employeeNumber);
        }

        if (facultyRepository.existsByUserUserId(userId)) {
            throw new ResourceConflictException(
                    "This user is already linked to a faculty account"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + departmentId));

        if (user.getRole() != User.Role.FACULTY) {
            throw new IllegalArgumentException("User role must be FACULTY to link a faculty profile");
        }

        Faculty faculty = new Faculty();

        faculty.setUser(user);
        faculty.setDepartment(department);
        faculty.setEmployeeNumber(employeeNumber);
        faculty.setFirstName(firstName);
        faculty.setLastName(lastName);
        faculty.setPhone(phone);
        faculty.setStatus(Faculty.Status.ACTIVE);

        Faculty saved = facultyRepository.save(faculty);

        auditLogService.record(
                "CREATE_FACULTY",
                "Faculty",
                saved.getFacultyId(),
                "Created faculty profile " + saved.getEmployeeNumber()
        );

        return saved;
    }

    public ResourceDependencySummary getFacultyDependencies(Long id) {
        Faculty faculty = getFacultyById(id);
        long sessions = attendanceSessionRepository != null ? attendanceSessionRepository.countByFacultyFacultyId(id) : 0;
        long assignments = facultyCourseAssignmentRepository != null
                ? facultyCourseAssignmentRepository.countByFacultyFacultyId(id) : 0;

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("attendanceSessions", sessions);
        counts.put("courseAssignments", assignments);

        boolean hasDependencies = (sessions > 0 || assignments > 0);
        boolean canDelete = !hasDependencies;
        boolean canArchive = true; // Faculty has status ACTIVE, INACTIVE

        String message = canDelete
                ? "Faculty member has no recorded attendance sessions or course assignments and can be safely deleted."
                : String.format("Faculty member has %d recorded attendance session(s) and %d course assignment(s). Deactivate faculty status instead of deleting.", sessions, assignments);

        return new ResourceDependencySummary("FACULTY", id, canDelete, canArchive, counts, message);
    }

    @Transactional
    public Faculty deactivateFaculty(Long id) {
        Faculty faculty = getFacultyById(id);
        faculty.setStatus(Faculty.Status.INACTIVE);
        Faculty saved = facultyRepository.save(faculty);

        auditLogService.record(
                "DEACTIVATE_FACULTY",
                "Faculty",
                saved.getFacultyId(),
                "Deactivated faculty profile " + saved.getEmployeeNumber()
        );

        return saved;
    }

    @Transactional
    public void deleteFaculty(Long id) {
        Faculty faculty = getFacultyById(id);
        requireVerifiableDependencies();
        ResourceDependencySummary deps = getFacultyDependencies(id);
        if (!deps.isCanDelete()) {
            throw new ResourceConflictException(deps.getMessage());
        }
        facultyRepository.delete(faculty);

        auditLogService.record(
                "DELETE_FACULTY",
                "Faculty",
                faculty.getFacultyId(),
                "Deleted faculty profile " + faculty.getEmployeeNumber()
        );
    }

    /**
     * Fail closed. Attendance sessions are the faculty member's teaching record,
     * so an absent repository must never be mistaken for "no sessions recorded".
     */
    private void requireVerifiableDependencies() {
        if (attendanceSessionRepository == null || facultyCourseAssignmentRepository == null) {

            throw new ResourceConflictException(
                    "Recorded attendance sessions and course assignments cannot be verified, "
                            + "so this faculty member cannot be deleted. "
                            + "Deactivate the faculty member instead to preserve the teaching record.");
        }
    }
}
