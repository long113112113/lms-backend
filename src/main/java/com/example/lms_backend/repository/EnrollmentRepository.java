package com.example.lms_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.enums.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByStudentIdAndCourseClassId(UUID studentId, UUID courseClassId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student WHERE e.courseClass.id = :courseClassId AND e.status = :status")
    List<Enrollment> findByCourseClassIdAndStatus(UUID courseClassId, EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.courseClass cc JOIN FETCH cc.course WHERE e.student.id = :studentId AND e.status = :status")
    List<Enrollment> findByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    boolean existsByStudentIdAndCourseClassId(UUID studentId, UUID courseClassId);
}
