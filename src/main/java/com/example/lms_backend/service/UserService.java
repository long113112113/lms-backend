package com.example.lms_backend.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.lms_backend.dto.user.CreateUserWithRoleRequest;
import com.example.lms_backend.dto.user.UpdateUserRequest;
import com.example.lms_backend.dto.user.UserResponse;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.exception.ResourceAlreadyExistsException;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.repository.UserRepository;
import com.example.lms_backend.repository.RefreshTokenRepository;
import com.example.lms_backend.specification.UserSpecification;
import java.time.Instant;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public UserResponse createUserWithRole(CreateUserWithRoleRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }
        var user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.valueOf(request.role()));
        var savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        var updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String email, String fullName, Role role, Pageable pageable) {
        Specification<User> spec = UserSpecification.build(email, fullName, role);
        return userRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenRepository.deleteByUser(user);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
