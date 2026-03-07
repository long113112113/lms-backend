package com.example.lms_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.enums.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID>, JpaSpecificationExecutor<Enrollment> {

    @Override
    @EntityGraph(attributePaths = { "student", "courseClass", "courseClass.course" })
    Page<Enrollment> findAll(Specification<Enrollment> spec, Pageable pageable);

    Optional<Enrollment> findByStudentIdAndCourseClassId(UUID studentId, UUID courseClassId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student WHERE e.courseClass.id = :courseClassId AND e.status = :status")
    List<Enrollment> findByCourseClassIdAndStatus(UUID courseClassId, EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.courseClass cc JOIN FETCH cc.course WHERE e.student.id = :studentId AND e.status = :status")
    List<Enrollment> findByStudentIdAndStatus(UUID studentId, EnrollmentStatus status);

    boolean existsByStudentIdAndCourseClassId(UUID studentId, UUID courseClassId);

    boolean existsByStudentIdAndCourseClassIdAndStatus(UUID studentId, UUID courseClassId, EnrollmentStatus status);

    @Query("SELECT e.courseClass.id, COUNT(e) FROM Enrollment e WHERE e.courseClass.id IN :classIds AND e.status = com.example.lms_backend.entity.enums.EnrollmentStatus.ACTIVE GROUP BY e.courseClass.id")
    List<Object[]> countActiveStudentsByClassIds(@Param("classIds") List<UUID> classIds);
}
