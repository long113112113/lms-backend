package com.example.lms_backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms_backend.dto.enrollment.EnrollmentResponse;
import com.example.lms_backend.dto.enrollment.JoinClassRequest;
import com.example.lms_backend.service.EnrollmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/join")
    public ResponseEntity<EnrollmentResponse> joinClass(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JoinClassRequest request) {
        UUID studentId = UUID.fromString(jwt.getSubject());
        var enrollment = enrollmentService.joinClass(studentId, request.joinCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{courseClassId}/leave")
    public ResponseEntity<Void> leaveClass(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID courseClassId) {
        UUID studentId = UUID.fromString(jwt.getSubject());
        enrollmentService.leaveClass(studentId, courseClassId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('TEACHER')")
    @DeleteMapping("/{courseClassId}/kick/{studentId}")
    public ResponseEntity<Void> kickStudent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID courseClassId,
            @PathVariable UUID studentId) {
        UUID teacherId = UUID.fromString(jwt.getSubject());
        enrollmentService.kickStudent(teacherId, courseClassId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-classes")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
            @AuthenticationPrincipal Jwt jwt) {
        UUID studentId = UUID.fromString(jwt.getSubject());
        var enrollments = enrollmentService.getMyEnrollments(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @GetMapping("/class/{courseClassId}/students")
    public ResponseEntity<List<EnrollmentResponse>> getClassStudents(
            @PathVariable UUID courseClassId) {
        var enrollments = enrollmentService.getClassStudents(courseClassId);
        return ResponseEntity.ok(enrollments);
    }
}
