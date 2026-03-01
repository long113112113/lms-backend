package com.example.lms_backend.dto.enrollment;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID enrollmentId,
        UUID studentId,
        String studentName,
        String studentEmail,
        UUID courseClassId,
        String courseClassCode,
        String courseName,
        String status,
        Instant createdAt) {
}
