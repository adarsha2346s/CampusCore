package com.campuscore.backend.repository;

import com.campuscore.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findByUserUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findByEntityName(String entityName, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findByAction(String action, Pageable pageable);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByEntityNameOrderByCreatedAtDesc(String entityName);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}
