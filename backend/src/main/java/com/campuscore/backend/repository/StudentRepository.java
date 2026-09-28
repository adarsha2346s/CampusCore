package com.campuscore.backend.repository;

import com.campuscore.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEnrollmentNumber(String enrollmentNumber);

    boolean existsByEnrollmentNumber(String enrollmentNumber);

    boolean existsByUserUserId(Long userId);

    Optional<Student> findByUserUserId(Long userId);
}
