package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "Refresh token cant be null") String refreshToken) {
}
