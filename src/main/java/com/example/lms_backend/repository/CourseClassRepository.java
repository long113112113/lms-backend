package com.example.lms_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.lms_backend.entity.CourseClass;

public interface CourseClassRepository extends JpaRepository<CourseClass, UUID> {
    boolean existsByCode(String code);

    boolean existsByJoinCode(String joinCode);

    Optional<CourseClass> findByJoinCodeAndIsDeletedFalse(String joinCode);

    @EntityGraph(attributePaths = { "course", "teacher" })
    Page<CourseClass> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = { "course", "teacher" })
    Page<CourseClass> findByTeacherId(UUID teacherId, Pageable pageable);

    @EntityGraph(attributePaths = { "course", "teacher" })
    @Query("SELECT cc FROM CourseClass cc JOIN Enrollment e ON e.courseClass = cc "
            + "WHERE e.student.id = :studentId AND e.status = com.example.lms_backend.entity.enums.EnrollmentStatus.ACTIVE")
    Page<CourseClass> findByEnrolledStudentId(UUID studentId, Pageable pageable);
}
