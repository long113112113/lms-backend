package com.example.lms_backend.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.lms_backend.validation.SafeHtml;

public record CourseRequest(
                @SafeHtml @NotBlank(message = "Course code cant be null") @Size(max = 20, message = "Course code must not exceed 20 characters") String code,
                @SafeHtml @NotBlank(message = "Subject name cant be null") @Size(max = 100, message = "Subject name must not exceed 100 characters") String name,
                @NotNull(message = "Credits cant be null") @Min(value = 1, message = "Credits must be at least 1") Integer credits,
                @SafeHtml @Size(max = 500, message = "Description must not exceed 500 characters") String description) {
}
