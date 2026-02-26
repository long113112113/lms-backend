package com.example.lms_backend.dto.user;

import java.time.Instant;
import java.util.UUID;

import com.example.lms_backend.entity.enums.Role;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        Instant createdAt,
        Instant updatedAt) {
}
