package com.campuscore.backend.controller;

import com.campuscore.backend.dto.AuditLogResponse;
import com.campuscore.backend.dto.PageResponse;
import com.campuscore.backend.lib.PageParameters;
import com.campuscore.backend.entity.AuditLog;
import com.campuscore.backend.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<?> getAllLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optionalNewest(page, size, "createdAt");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(auditLogService.getLogsPage(pageable), this::toResponse));
        }

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
    public ResponseEntity<?> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optionalNewest(page, size, "createdAt");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    auditLogService.getLogsByUserPage(userId, pageable), this::toResponse));
        }

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
    public ResponseEntity<?> getLogsByEntity(
            @PathVariable String entityName,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optionalNewest(page, size, "createdAt");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    auditLogService.getLogsByEntityPage(entityName, pageable), this::toResponse));
        }

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
    public ResponseEntity<?> getLogsByAction(
            @PathVariable String action,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Pageable pageable = PageParameters.optionalNewest(page, size, "createdAt");
        if (pageable != null) {
            return ResponseEntity.ok(PageResponse.from(
                    auditLogService.getLogsByActionPage(action, pageable), this::toResponse));
        }

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
