package com.example.lms_backend.dto.course;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.lms_backend.validation.SafeHtml;

public record CreateCourseClassRequest(
        @NotNull(message = "Course ID is required") UUID courseId,
        @SafeHtml @NotBlank(message = "Class code is required") @Size(max = 50, message = "Class code cannot exceed 50 characters") String code,
        @SafeHtml @NotBlank(message = "Semester is required") @Size(max = 20, message = "Semester cannot exceed 20 characters") String semester,
        @NotNull(message = "Teacher ID is required") UUID teacherId) {
}
