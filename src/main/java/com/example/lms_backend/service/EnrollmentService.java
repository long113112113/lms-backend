package com.example.lms_backend.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.example.lms_backend.specification.EnrollmentSpecification;

@Service
public class EnrollmentService {
    private static final List<String> ALLOWED_CLASS_STUDENT_SORT_FIELDS = List.of(
            "createdAt",
            "studentName",
            "studentEmail");
    private static final Sort DEFAULT_CLASS_STUDENT_SORT = Sort.by(Sort.Order.desc("createdAt"));
    private static final Map<String, String> CLASS_STUDENT_SORT_FIELDS = Map.of(
            "createdAt", "createdAt",
            "studentName", "student.fullName",
            "studentEmail", "student.email");

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
        CourseClass courseClass = courseClassRepository.findByIdAndIsDeletedFalse(courseClassId)
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
    public Page<EnrollmentResponse> getClassStudents(UUID courseClassId, UUID callerId, String role, String q,
            Pageable pageable) {
        CourseClass courseClass = courseClassRepository.findByIdAndIsDeletedFalse(courseClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!"ADMIN".equals(role)) {
            if (courseClass.getTeacher() == null || !courseClass.getTeacher().getId().equals(callerId)) {
                throw new AccessDeniedException("You are not the teacher of this class");
            }
        }

        Pageable sanitizedPageable = sanitizeClassStudentsPageable(pageable);
        return enrollmentRepository
                .findAll(EnrollmentSpecification.buildForClassStudents(courseClassId, q), sanitizedPageable)
                .map(this::mapToResponse);
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

    private Pageable sanitizeClassStudentsPageable(Pageable pageable) {
        Sort requestedSort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_CLASS_STUDENT_SORT;
        List<Sort.Order> translatedOrders = requestedSort.stream()
                .map(this::translateClassStudentSortOrder)
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(translatedOrders));
    }

    private Sort.Order translateClassStudentSortOrder(Sort.Order order) {
        String mappedProperty = CLASS_STUDENT_SORT_FIELDS.get(order.getProperty());
        if (mappedProperty == null) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: %s. Allowed values: %s"
                            .formatted(order.getProperty(), String.join(", ", ALLOWED_CLASS_STUDENT_SORT_FIELDS)));
        }
        return order.withProperty(mappedProperty);
    }
}
