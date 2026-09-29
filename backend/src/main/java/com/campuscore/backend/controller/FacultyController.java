package com.campuscore.backend.controller;

import com.campuscore.backend.dto.FacultyResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.dto.FacultySelfResponse;
import com.campuscore.backend.entity.Faculty;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.FacultyRepository;
import com.campuscore.backend.repository.UserRepository;
import com.campuscore.backend.service.FacultyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;

    public FacultyController(
            FacultyService facultyService,
            UserRepository userRepository,
            FacultyRepository facultyRepository) {

        this.facultyService = facultyService;
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<FacultySelfResponse> getMyFacultyProfile(
            Authentication authentication) {

        boolean isFaculty = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_FACULTY"));
        if (!isFaculty) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Faculty faculty = facultyService.getFacultyForUsername(authentication.getName());
        FacultySelfResponse response = new FacultySelfResponse(
                faculty.getEmployeeNumber(),
                faculty.getFirstName(),
                faculty.getLastName(),
                faculty.getDepartment().getName(),
                faculty.getStatus().name()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllFaculty(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optional(page, size, "facultyId");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    facultyService.getFacultyPage(pageable), this::toResponse));
        }

        List<FacultyResponse> faculty =
                facultyService.getAllFaculty()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(faculty);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getFacultyById(
            @PathVariable Long id,
            Authentication authentication) {

        Faculty faculty = facultyService.getFacultyById(id);

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {

            User user = userRepository
                    .findByUsername(authentication.getName())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Authenticated user not found"
                            )
                    );

            Faculty loggedInFaculty = facultyRepository
                    .findByUserUserId(user.getUserId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Faculty record not found"
                            )
                    );

            if (!loggedInFaculty.getFacultyId().equals(id)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .build();
            }
        }

        return ResponseEntity.ok(
                toResponse(faculty)
        );
    }

    @PostMapping
    public ResponseEntity<FacultyResponse> createFaculty(
            @RequestParam Long userId,
            @RequestParam Long departmentId,
            @RequestParam String employeeNumber,
            @RequestParam String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone) {

        Faculty faculty = facultyService.createFaculty(
                userId,
                departmentId,
                employeeNumber,
                firstName,
                lastName,
                phone
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(faculty));
    }

    private FacultyResponse toResponse(Faculty faculty) {

        return new FacultyResponse(
                faculty.getFacultyId(),
                faculty.getUser().getUserId(),
                faculty.getDepartment().getDepartmentId(),
                faculty.getEmployeeNumber(),
                faculty.getFirstName(),
                faculty.getLastName(),
                faculty.getPhone(),
                faculty.getStatus().name()
        );
    }
}
