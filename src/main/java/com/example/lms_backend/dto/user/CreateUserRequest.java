package com.example.lms_backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") String email,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Role is required") String role) {
}
