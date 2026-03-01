package com.example.lms_backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.enrollment.EnrollmentResponse;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.entity.Enrollment;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.EnrollmentStatus;
import com.example.lms_backend.exception.AccessDeniedException;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.EnrollmentRepository;
import com.example.lms_backend.repository.UserRepository;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseClassRepository courseClassRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
            CourseClassRepository courseClassRepository,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseClassRepository = courseClassRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnrollmentResponse joinClass(UUID studentId, String joinCode) {
        CourseClass courseClass = courseClassRepository.findByJoinCodeAndIsDeletedFalse(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid join code"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        var existingEnrollment = enrollmentRepository
                .findByStudentIdAndCourseClassId(studentId, courseClass.getId());

        if (existingEnrollment.isPresent()) {
            Enrollment enrollment = existingEnrollment.get();
            switch (enrollment.getStatus()) {
                case ACTIVE -> throw new ResourceAlreadyExistsException("You have already joined this class");
                case KICKED ->
                    throw new AccessDeniedException("You have been removed from this class");
                case LEFT -> {
                    enrollment.setStatus(EnrollmentStatus.ACTIVE);
                    Enrollment saved = enrollmentRepository.save(enrollment);
                    return mapToResponse(saved);
                }
                case COMPLETED -> throw new ResourceAlreadyExistsException("You have already completed this class");
            }
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourseClass(courseClass);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        Enrollment saved = enrollmentRepository.save(enrollment);
        return mapToResponse(saved);
    }

    @Transactional
    public void leaveClass(UUID studentId, UUID courseClassId) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseClassId(studentId, courseClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ResourceNotFoundException("No active enrollment found for this class");
        }

        enrollment.setStatus(EnrollmentStatus.LEFT);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void kickStudent(UUID teacherId, UUID courseClassId, UUID studentId) {
        CourseClass courseClass = courseClassRepository.findById(courseClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (courseClass.getTeacher() == null || !courseClass.getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("You are not the teacher of this class");
        }

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseClassId(studentId, courseClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Student is not enrolled in this class"));

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ResourceNotFoundException("No active enrollment found for this student");
        }

        enrollment.setStatus(EnrollmentStatus.KICKED);
        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getClassStudents(UUID courseClassId, UUID callerId, String role) {
        CourseClass courseClass = courseClassRepository.findById(courseClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!"ADMIN".equals(role)) {
            if (courseClass.getTeacher() == null || !courseClass.getTeacher().getId().equals(callerId)) {
                throw new AccessDeniedException("You are not the teacher of this class");
            }
        }

        List<Enrollment> enrollments = enrollmentRepository
                .findByCourseClassIdAndStatus(courseClassId, EnrollmentStatus.ACTIVE);
        return enrollments.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE);
        return enrollments.stream().map(this::mapToResponse).toList();
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        User student = enrollment.getStudent();
        CourseClass courseClass = enrollment.getCourseClass();
        Course course = courseClass.getCourse();
        return new EnrollmentResponse(
                enrollment.getId(),
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                courseClass.getId(),
                courseClass.getCode(),
                course.getName(),
                enrollment.getStatus().name(),
                enrollment.getCreatedAt());
    }
}
