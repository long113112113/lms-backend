package com.example.lms_backend.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.lms_backend.validation.SafeHtml;

public record CourseRequest(
        @SafeHtml @NotBlank(message = "Course code cant be null") String code,
        @SafeHtml @NotBlank(message = "Subject name cant be null") String name,
        @NotNull(message = "Credits cant be null") @Min(value = 1, message = "Credits must be at least 1") Integer credits,
        @SafeHtml String description) {
}
