package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Student> findAll(Pageable pageable);

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    boolean existsByUserUserId(Long userId);

    @EntityGraph(attributePaths = "department")
    Optional<Student> findByUserUserId(Long userId);
}
