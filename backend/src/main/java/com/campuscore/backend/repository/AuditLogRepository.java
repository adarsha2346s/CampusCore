package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByEntityNameOrderByCreatedAtDesc(String entityName);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}