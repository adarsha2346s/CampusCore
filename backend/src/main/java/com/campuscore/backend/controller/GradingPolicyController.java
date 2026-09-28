package com.campuscore.backend.controller;

import com.campuscore.backend.dto.GradingPolicyRequest;
import com.campuscore.backend.entity.GradingPolicy;
import com.campuscore.backend.service.GradingPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grading-policies")
public class GradingPolicyController {

    private final GradingPolicyService gradingPolicyService;

    public GradingPolicyController(GradingPolicyService gradingPolicyService) {
        this.gradingPolicyService = gradingPolicyService;
    }

    @PostMapping
    public ResponseEntity<GradingPolicy> createPolicy(
            @Valid @RequestBody GradingPolicyRequest request) {

        return ResponseEntity.ok(
                gradingPolicyService.createPolicy(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<GradingPolicy>> getAllPolicies() {

        return ResponseEntity.ok(
                gradingPolicyService.getAllPolicies()
        );
    }
}
