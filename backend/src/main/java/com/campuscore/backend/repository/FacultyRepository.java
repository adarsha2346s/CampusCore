package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Faculty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Faculty> findAll(Pageable pageable);

    Optional<Faculty> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByUserUserId(Long userId);

    @EntityGraph(attributePaths = "department")
    Optional<Faculty> findByUserUserId(Long userId);
}
