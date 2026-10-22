package com.campuscore.backend.service;

import com.campuscore.backend.dto.GradingPolicyRequest;
import com.campuscore.backend.entity.GradingPolicy;
import com.campuscore.backend.repository.GradingPolicyRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradingPolicyService {

    private final GradingPolicyRepository gradingPolicyRepository;
    private final AuditLogService auditLogService;

    public GradingPolicyService(
            GradingPolicyRepository gradingPolicyRepository,
            AuditLogService auditLogService) {
        this.gradingPolicyRepository = gradingPolicyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public GradingPolicy createPolicy(GradingPolicyRequest request) {

        if (request.getMinPercentage().compareTo(request.getMaxPercentage()) > 0) {
            throw new RuntimeException(
                    "Minimum percentage cannot be greater than maximum percentage"
            );
        }

        GradingPolicy policy = new GradingPolicy();

        policy.setName(request.getName());
        policy.setMinPercentage(request.getMinPercentage());
        policy.setMaxPercentage(request.getMaxPercentage());
        policy.setGrade(request.getGrade());
        policy.setGradePoint(request.getGradePoint());

        GradingPolicy saved = gradingPolicyRepository.save(policy);

        auditLogService.record(
                "CREATE_GRADING_POLICY",
                "GradingPolicy",
                saved.getGradingPolicyId(),
                "Created grading policy " + saved.getName() + " for grade "
                        + saved.getGrade()
        );

        return saved;
    }

    public List<GradingPolicy> getAllPolicies() {
        return gradingPolicyRepository.findAll();
    }
}