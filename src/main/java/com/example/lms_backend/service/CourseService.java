package com.example.lms_backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.course.CourseRequest;
import com.example.lms_backend.dto.course.CourseResponse;
import com.example.lms_backend.entity.Course;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.CourseRepository;
import java.util.UUID;

import com.example.lms_backend.specification.CourseSpecification;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(String code, String name, Integer minCredits, Integer maxCredits,
            Pageable pageable) {
        Specification<Course> spec = CourseSpecification.build(code, name, minCredits, maxCredits);
        return courseRepository.findAll(spec, pageable).map(this::mapToResponse);
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

    @Transactional
    public CourseResponse updateCourse(UUID id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (!course.getCode().equals(request.code()) && courseRepository.existsByCode(request.code())) {
            throw new ResourceAlreadyExistsException("Course code already exists: " + request.code());
        }

        course.setCode(request.code());
        course.setName(request.name());
        course.setCredits(request.credits());
        course.setDescription(request.description());

        var updatedCourse = courseRepository.save(course);
        return mapToResponse(updatedCourse);
    }

    @Transactional
    public void deleteCourse(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    private CourseResponse mapToResponse(Course course) {
        return new CourseResponse(course.getId(), course.getCode(), course.getName(), course.getCredits(),
                course.getDescription(), course.getCreatedAt(), course.getUpdatedAt());
    }
}