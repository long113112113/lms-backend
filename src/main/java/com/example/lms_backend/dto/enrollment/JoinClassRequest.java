package com.example.lms_backend.dto.enrollment;

import com.example.lms_backend.validation.SafeHtml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinClassRequest(
        // SECURITY: Add size validation to prevent DoS attacks with giant strings
        @SafeHtml @NotBlank(message = "Join code is required") @Size(max = 20, message = "Join code cannot exceed 20 characters") String joinCode) {
}
