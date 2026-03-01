package com.example.lms_backend.service;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.course.CourseClassResponse;
import com.example.lms_backend.dto.course.CreateCourseClassRequest;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.exception.AccessDeniedException;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.UserRepository;

@Service
public class CourseClassService {
    private static final String JOIN_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int JOIN_CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CourseClassRepository courseClassRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseClassService(CourseClassRepository courseClassRepository, CourseRepository courseRepository,
            UserRepository userRepository) {
        this.courseClassRepository = courseClassRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CourseClassResponse createCourseClass(CreateCourseClassRequest request) {
        if (courseClassRepository.existsByCode(request.code())) {
            throw new ResourceAlreadyExistsException("Course class code already exists");
        }
        var course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        var courseClass = new CourseClass();
        courseClass.setCourse(course);
        courseClass.setCode(request.code());
        courseClass.setSemester(request.semester());
        courseClass.setJoinCode(generateJoinCode());

        if (request.teacherId() != null) {
            var teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
            courseClass.setTeacher(teacher);
        }

        var savedCourseClass = courseClassRepository.save(courseClass);
        return mapToResponse(savedCourseClass);
    }

    @Transactional
    public CourseClassResponse resetJoinCode(UUID teacherId, UUID courseClassId) {
        var courseClass = courseClassRepository.findById(courseClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (courseClass.getTeacher() == null || !courseClass.getTeacher().getId().equals(teacherId)) {
            throw new AccessDeniedException("You are not the teacher of this class");
        }

        courseClass.setJoinCode(generateJoinCode());
        var saved = courseClassRepository.save(courseClass);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CourseClassResponse> getAllCourseClasses(Pageable pageable) {
        return courseClassRepository.findAllWithCourse(pageable).map(this::mapToResponse);
    }

    private String generateJoinCode() {
        String code;
        do {
            var sb = new StringBuilder(JOIN_CODE_LENGTH);
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                sb.append(JOIN_CODE_CHARS.charAt(RANDOM.nextInt(JOIN_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (courseClassRepository.existsByJoinCode(code));
        return code;
    }

    private CourseClassResponse mapToResponse(CourseClass courseClass) {
        var teacher = courseClass.getTeacher();
        return new CourseClassResponse(
                courseClass.getId(),
                courseClass.getCode(),
                courseClass.getSemester(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getFullName() : null,
                courseClass.getCourse().getId(),
                courseClass.getCourse().getName(),
                courseClass.getJoinCode(),
                courseClass.getCreatedAt(),
                courseClass.getUpdatedAt());
    }
}
