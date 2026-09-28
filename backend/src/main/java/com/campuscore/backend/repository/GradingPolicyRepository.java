package com.campuscore.backend.repository;

import com.campuscore.backend.entity.GradingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface GradingPolicyRepository
        extends JpaRepository<GradingPolicy, Long> {

    @Query("""
        SELECT g
        FROM GradingPolicy g
        WHERE g.name = :name
          AND g.minPercentage <= :percentage
          AND g.maxPercentage >= :percentage
        """)
    Optional<GradingPolicy> findMatchingPolicy(
            @Param("name") String name,
            @Param("percentage") BigDecimal percentage
    );
}