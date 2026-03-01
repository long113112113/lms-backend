package com.example.lms_backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms_backend.dto.course.CourseClassResponse;
import com.example.lms_backend.dto.course.CreateCourseClassRequest;
import com.example.lms_backend.service.CourseClassService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/course-classes")
public class CourseClassController {
    private final CourseClassService courseClassService;

    public CourseClassController(CourseClassService courseClassService) {
        this.courseClassService = courseClassService;
    }

    @GetMapping
    public ResponseEntity<Page<CourseClassResponse>> getAllCourseClasses(Pageable pageable) {
        return ResponseEntity.ok(courseClassService.getAllCourseClasses(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CourseClassResponse> createCourseClass(
            @Valid @RequestBody CreateCourseClassRequest request) {
        var newCourseClass = courseClassService.createCourseClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newCourseClass);
    }

    @PreAuthorize("hasRole('TEACHER')")
    @PutMapping("/{id}/reset-join-code")
    public ResponseEntity<CourseClassResponse> resetJoinCode(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID teacherId = UUID.fromString(jwt.getSubject());
        var updatedClass = courseClassService.resetJoinCode(teacherId, id);
        return ResponseEntity.ok(updatedClass);
    }
}
