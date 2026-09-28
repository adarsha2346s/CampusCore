package com.campuscore.backend.dto;

public class AuditLogResponse {

    private Long auditLogId;
    private Long userId;
    private String action;
    private String entityName;
    private Long entityId;
    private String description;
    private String createdAt;

    public AuditLogResponse(
            Long auditLogId,
            Long userId,
            String action,
            String entityName,
            Long entityId,
            String description,
            String createdAt) {

        this.auditLogId = auditLogId;
        this.userId = userId;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getAuditLogId() {
        return auditLogId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}