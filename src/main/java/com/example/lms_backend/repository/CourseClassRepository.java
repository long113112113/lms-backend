package com.example.lms_backend.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.lms_backend.entity.CourseClass;

public interface CourseClassRepository extends JpaRepository<CourseClass, UUID> {
    boolean existsByCode(String code);

    @Query("SELECT cc FROM CourseClass cc JOIN FETCH cc.course LEFT JOIN FETCH cc.teacher")
    Page<CourseClass> findAllWithCourse(Pageable pageable);
}
