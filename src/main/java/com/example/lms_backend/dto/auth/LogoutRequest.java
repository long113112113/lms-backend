package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

import com.example.lms_backend.validation.SafeHtml;

public record LogoutRequest(
                @SafeHtml @NotBlank(message = "Device ID cant be null") String deviceId) {
}
