package com.example.lms_backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.lms_backend.validation.SafeHtml;

public record CreateUserRequest(
        @SafeHtml @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") @Size(max = 100, message = "Email cannot exceed 100 characters") String email,
        @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters") String password,
        @SafeHtml @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name cannot exceed 100 characters") String fullName) {
}
