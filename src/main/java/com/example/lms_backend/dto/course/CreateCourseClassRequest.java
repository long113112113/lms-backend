package com.example.lms_backend.dto.course;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCourseClassRequest(
        @NotNull(message = "Course ID is required") UUID courseId,
        @NotBlank(message = "Class code is required") String code,
        @NotBlank(message = "Semester is required") String semester,
        @NotNull(message = "Teacher ID is required") UUID teacherId) {
}
