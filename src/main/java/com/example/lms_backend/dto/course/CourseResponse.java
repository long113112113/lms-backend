package com.example.lms_backend.dto.course;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(UUID id, String code, String name, Integer credits, String description, Instant createdAt,
                Instant updatedAt) {
}
