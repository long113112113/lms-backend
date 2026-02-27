package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.lms_backend.validation.SafeHtml;

public record LoginRequest(
                @SafeHtml @NotBlank(message = "Email cant be null")
                @Size(max = 100, message = "Email cannot exceed 100 characters")
                String email,

                @NotBlank(message = "Password cant be null")
                @Size(max = 72, message = "Password cannot exceed 72 characters")
                String password,

                @SafeHtml @NotBlank(message = "Device ID cant be null")
                @Size(max = 100, message = "Device ID cannot exceed 100 characters")
                String deviceId,

                @SafeHtml @Size(max = 100, message = "Device Name cannot exceed 100 characters")
                String deviceName) {
}
