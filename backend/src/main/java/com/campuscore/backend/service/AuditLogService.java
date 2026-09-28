package com.campuscore.backend.service;

import com.campuscore.backend.entity.AuditLog;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.AuditLogRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {

        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    // Create an audit log
    public AuditLog createLog(
            Long userId,
            String action,
            String entityName,
            Long entityId,
            String description) {

        User user = null;

        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "User not found with ID: " + userId
                            )
                    );
        }

        AuditLog auditLog = new AuditLog();

        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);

        return auditLogRepository.save(auditLog);
    }

    // Get all audit logs
    public List<AuditLog> getAllLogs() {
        return auditLogRepository
                .findAllByOrderByCreatedAtDesc();
    }

    // Get audit logs for a specific user
    public List<AuditLog> getLogsByUser(Long userId) {
        return auditLogRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    // Get audit logs for a specific entity
    public List<AuditLog> getLogsByEntity(
            String entityName) {

        return auditLogRepository
                .findByEntityNameOrderByCreatedAtDesc(entityName);
    }

    // Get audit logs by action
    public List<AuditLog> getLogsByAction(
            String action) {

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(action);
    }
}
