package com.example.lms_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.course.CourseRequest;
import com.example.lms_backend.dto.course.CourseResponse;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.repository.CourseRepository;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByCode(request.code())) {
            throw new ResourceAlreadyExistsException("Course code already exists: " + request.code());
        }
        var course = new Course();
        course.setCode(request.code());
        course.setName(request.name());
        course.setCredits(request.credits());
        course.setDescription(request.description());
        var savedCourse = courseRepository.save(course);
        return mapToResponse(savedCourse);
    }

    private CourseResponse mapToResponse(Course course) {
        return new CourseResponse(course.getId(), course.getCode(), course.getName(), course.getCredits(),
                course.getDescription(), course.getCreatedAt(), course.getUpdatedAt());
    }
}