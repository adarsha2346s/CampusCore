package com.campuscore.backend.dto;

import java.util.Collections;
import java.util.Map;

public class ResourceDependencySummary {

    private String resourceType;
    private Long resourceId;
    private boolean canDelete;
    private boolean canArchive;
    private Map<String, Long> dependencyCounts;
    private String message;

    public ResourceDependencySummary() {
    }

    public ResourceDependencySummary(
            String resourceType,
            Long resourceId,
            boolean canDelete,
            boolean canArchive,
            Map<String, Long> dependencyCounts,
            String message) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.canDelete = canDelete;
        this.canArchive = canArchive;
        this.dependencyCounts = dependencyCounts != null ? dependencyCounts : Collections.emptyMap();
        this.message = message;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    public boolean isCanArchive() {
        return canArchive;
    }

    public void setCanArchive(boolean canArchive) {
        this.canArchive = canArchive;
    }

    public Map<String, Long> getDependencyCounts() {
        return dependencyCounts;
    }

    public void setDependencyCounts(Map<String, Long> dependencyCounts) {
        this.dependencyCounts = dependencyCounts;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
