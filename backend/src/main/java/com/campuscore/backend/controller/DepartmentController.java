package com.campuscore.backend.controller;

import com.campuscore.backend.dto.DepartmentRequest;
import com.campuscore.backend.entity.Department;
import com.campuscore.backend.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // GET all departments
    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(
                departmentService.getAllDepartments()
        );
    }

    // GET department by ID
    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                departmentService.getDepartmentById(id)
        );
    }

    // CREATE department
    @PostMapping
    public ResponseEntity<?> createDepartment(
            @Valid @RequestBody DepartmentRequest request,
            BindingResult bindingResult) {

        // Check validation errors
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

        Department department = new Department();

        department.setName(request.getName());
        department.setCode(request.getCode());

        Department savedDepartment =
                departmentService.createDepartment(department);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedDepartment);
    }

    // UPDATE department
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request,
            BindingResult bindingResult) {

        // Check validation errors
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

        Department department = new Department();

        department.setName(request.getName());
        department.setCode(request.getCode());

        return ResponseEntity.ok(
                departmentService.updateDepartment(id, department)
        );
    }

    // DELETE department
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(
            @PathVariable Long id) {

        departmentService.deleteDepartment(id);

        return ResponseEntity.noContent().build();
    }
}