package com.campuscore.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "grading_policy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grading_range",
                        columnNames = {"name", "min_percentage", "max_percentage"}
                )
        }
)
public class GradingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grading_policy_id")
    private Long gradingPolicyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(nullable = false, length = 5)
    private String grade;

    @Column(name = "grade_point", nullable = false, precision = 4, scale = 2)
    private BigDecimal gradePoint;

    public GradingPolicy() {
    }

    public Long getGradingPolicyId() {
        return gradingPolicyId;
    }

    public void setGradingPolicyId(Long gradingPolicyId) {
        this.gradingPolicyId = gradingPolicyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMinPercentage() {
        return minPercentage;
    }

    public void setMinPercentage(BigDecimal minPercentage) {
        this.minPercentage = minPercentage;
    }

    public BigDecimal getMaxPercentage() {
        return maxPercentage;
    }

    public void setMaxPercentage(BigDecimal maxPercentage) {
        this.maxPercentage = maxPercentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public BigDecimal getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(BigDecimal gradePoint) {
        this.gradePoint = gradePoint;
    }
}