package com.example.lms_backend.dto.course;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.lms_backend.validation.SafeHtml;

public record CreateCourseClassRequest(
                @NotNull(message = "Course ID is required") UUID courseId,
                @SafeHtml @NotBlank(message = "Class code is required") String code,
                @SafeHtml @NotBlank(message = "Semester is required") String semester,
                @NotNull(message = "Teacher ID is required") UUID teacherId) {
}
