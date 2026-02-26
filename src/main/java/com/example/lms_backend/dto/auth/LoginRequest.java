package com.example.lms_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Email cant be null") String email,
        @NotBlank(message = "Password cant be null") String password) {

}
