package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "Device ID cant be null") String deviceId) {
}
