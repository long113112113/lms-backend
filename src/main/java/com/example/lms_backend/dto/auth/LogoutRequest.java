package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.lms_backend.validation.SafeHtml;

public record LogoutRequest(
                // SECURITY: Limit input size to prevent DoS via massive strings
                @SafeHtml @NotBlank(message = "Device ID cant be null")
                @Size(max = 100, message = "Device ID cannot exceed 100 characters")
                String deviceId) {
}
