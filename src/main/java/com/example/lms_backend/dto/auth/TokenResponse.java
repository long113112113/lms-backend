package com.example.lms_backend.dto.auth;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

}
