package com.example.lms_backend.dto.enrollment;

import com.example.lms_backend.validation.SafeHtml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JoinClassRequest(
        @SafeHtml
        @NotBlank(message = "Join code is required")
        @Pattern(regexp = "^[A-Z0-9]{7}$", message = "Join code must be exactly 7 uppercase letters or digits")
        String joinCode) {
}
