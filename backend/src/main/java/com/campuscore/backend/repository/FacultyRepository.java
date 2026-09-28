package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByUserUserId(Long userId);

    Optional<Faculty> findByUserUserId(Long userId);
}