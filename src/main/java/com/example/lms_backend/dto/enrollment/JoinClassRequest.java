package com.example.lms_backend.dto.enrollment;

import com.example.lms_backend.validation.SafeHtml;

import jakarta.validation.constraints.NotBlank;

public record JoinClassRequest(
        @SafeHtml @NotBlank(message = "Join code is required") String joinCode) {
}
