package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

import com.example.lms_backend.validation.SafeHtml;

public record LoginRequest(
        @SafeHtml @NotBlank(message = "Email cant be null") String email,
        @NotBlank(message = "Password cant be null") String password,
        @SafeHtml @NotBlank(message = "Device ID cant be null") String deviceId,
        @SafeHtml String deviceName) {
}
