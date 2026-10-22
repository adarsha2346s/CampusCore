package com.campuscore.backend.service;

import com.campuscore.backend.entity.AuditLog;
import com.campuscore.backend.entity.User;
import com.campuscore.backend.repository.AuditLogRepository;
import com.campuscore.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * Records an administrative action performed by the signed-in account.
     *
     * <p>The actor is read from the authenticated security context, never from
     * the request, so a caller cannot record an action as somebody else and a
     * forged actor cannot be smuggled in through a request body.
     *
     * <p>This is the single entry point the services use. Logging an action in
     * more than one place would produce a second row for the same event, and
     * logging before the write succeeded would leave a record of something
     * that never happened.
     */
    public AuditLog record(
            String action,
            String entityName,
            Long entityId,
            String description) {

        return createLog(
                currentActorId(),
                action,
                entityName,
                entityId,
                description);
    }

    /**
     * The account that is performing the current action.
     *
     * <p>Null when there is no authenticated account, which is the case for
     * work that is not driven by a signed-in user. The audit row is still
     * written; the schema allows a row without an actor.
     */
    private Long currentActorId() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            return null;
        }

        return userRepository
                .findByUsername(authentication.getName())
                .map(User::getUserId)
                .orElse(null);
    }

    // Get all audit logs
    public List<AuditLog> getAllLogs() {
        return auditLogRepository
                .findAllByOrderByCreatedAtDesc();
    }

    public Page<AuditLog> getLogsPage(Pageable pageable) {
        return auditLogRepository.findAllBy(pageable);
    }

    // Get audit logs for a specific user
    public List<AuditLog> getLogsByUser(Long userId) {
        return auditLogRepository
                .findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<AuditLog> getLogsByUserPage(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserUserId(userId, pageable);
    }

    // Get audit logs for a specific entity
    public List<AuditLog> getLogsByEntity(
            String entityName) {

        return auditLogRepository
                .findByEntityNameOrderByCreatedAtDesc(entityName);
    }

    public Page<AuditLog> getLogsByEntityPage(String entityName, Pageable pageable) {
        return auditLogRepository.findByEntityName(entityName, pageable);
    }

    // Get audit logs by action
    public List<AuditLog> getLogsByAction(
            String action) {

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(action);
    }

    public Page<AuditLog> getLogsByActionPage(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
}
