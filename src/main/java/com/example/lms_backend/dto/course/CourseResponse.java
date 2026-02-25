package com.example.lms_backend.dto.course;

import java.time.Instant;

public record CourseResponse(Long id, String code, String name, Integer credits, String description, Instant createdAt,
        Instant updatedAt) {
}
