package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AuditLogResponse;
import com.campuscore.backend.entity.AuditLog;
import com.campuscore.backend.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // =========================
    // GET ALL AUDIT LOGS
    // =========================
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAllLogs() {

        List<AuditLogResponse> response =
                auditLogService.getAllLogs()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // GET LOGS BY USER
    // =========================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByUser(
            @PathVariable Long userId) {

        List<AuditLogResponse> response =
                auditLogService.getLogsByUser(userId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // GET LOGS BY ENTITY
    // =========================
    @GetMapping("/entity/{entityName}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByEntity(
            @PathVariable String entityName) {

        List<AuditLogResponse> response =
                auditLogService.getLogsByEntity(entityName)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // GET LOGS BY ACTION
    // =========================
    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByAction(
            @PathVariable String action) {

        List<AuditLogResponse> response =
                auditLogService.getLogsByAction(action)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =========================
    // ENTITY → RESPONSE DTO
    // =========================
    private AuditLogResponse toResponse(AuditLog auditLog) {

        Long userId = null;

        if (auditLog.getUser() != null) {
            userId = auditLog.getUser().getUserId();
        }

        String createdAt = null;

        if (auditLog.getCreatedAt() != null) {
            createdAt = auditLog.getCreatedAt().toString();
        }

        return new AuditLogResponse(
                auditLog.getAuditLogId(),
                userId,
                auditLog.getAction(),
                auditLog.getEntityName(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                createdAt
        );
    }
}
