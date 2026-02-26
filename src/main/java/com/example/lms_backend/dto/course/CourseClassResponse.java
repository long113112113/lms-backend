package com.example.lms_backend.dto.course;

import java.time.Instant;
import java.util.UUID;

public record CourseClassResponse(
        UUID id,
        String code,
        String semester,
        UUID teacherId,
        UUID courseId,
        String courseName,
        Instant createdAt,
        Instant updatedAt) {
}
