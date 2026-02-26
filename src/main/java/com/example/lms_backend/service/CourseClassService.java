package com.example.lms_backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.course.CourseClassResponse;
import com.example.lms_backend.dto.course.CreateCourseClassRequest;
import com.example.lms_backend.entity.CourseClass;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseClassRepository;
import com.example.lms_backend.repository.CourseRepository;
import com.example.lms_backend.repository.UserRepository;

@Service
public class CourseClassService {
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

        if (request.teacherId() != null) {
            var teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
            courseClass.setTeacher(teacher);
        }

        var savedCourseClass = courseClassRepository.save(courseClass);
        return mapToResponse(savedCourseClass);
    }

    @Transactional(readOnly = true)
    public Page<CourseClassResponse> getAllCourseClasses(Pageable pageable) {
        return courseClassRepository.findAllWithCourse(pageable).map(this::mapToResponse);
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
                courseClass.getCreatedAt(),
                courseClass.getUpdatedAt());
    }
}
