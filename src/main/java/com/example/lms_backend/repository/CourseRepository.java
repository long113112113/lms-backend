package com.example.lms_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.lms_backend.entity.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    boolean existsByCode(String code);
}
