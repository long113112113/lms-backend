package com.example.lms_backend.dto.user;

import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.validation.SafeHtml;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @SafeHtml @NotBlank(message = "Email is required") @Email(message = "Email format is invalid") String email,
        @SafeHtml @NotBlank(message = "Full name is required") String fullName,
        @NotNull(message = "Role is required") Role role,
        String password) {
}
