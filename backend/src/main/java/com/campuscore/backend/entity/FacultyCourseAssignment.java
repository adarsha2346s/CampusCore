package com.campuscore.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Normalised faculty-to-course assignment.
 *
 * <p>A faculty member may teach many courses and a course may be co-taught, so
 * this is deliberately a relationship table rather than a single faculty_id on
 * {@link Course}.
 *
 * <p>Rows are never overwritten on reassignment: the previous row is closed by
 * setting {@code endedAt}, which keeps the teaching history of a course intact.
 * A row with {@code endedAt == null} is the current assignment.
 *
 * <p>Associations are unidirectional on purpose. Existing endpoints serialise
 * entities directly in places, and a back reference would risk lazy-loading
 * failures and unbounded recursion.
 */
@Entity
@Table(name = "faculty_course_assignment")
public class FacultyCourseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public FacultyCourseAssignment() {
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }

    /** True while this assignment is the current one. */
    public boolean isActive() {
        return endedAt == null;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
