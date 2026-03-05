package com.example.lms_backend.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.lms_backend.specification.UserSpecification;

@Service
public class UserService {
    // SECURITY: Audit logging added to track administrative actions
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUserWithRole(CreateUserWithRoleRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            securityLog.warn("ADMIN_ACTION action=createUser result=failure reason=email_exists email={}", request.email());
            throw new ResourceAlreadyExistsException("Email already exists");
        }
        var user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.valueOf(request.role()));
        var savedUser = userRepository.save(user);
        securityLog.info("ADMIN_ACTION action=createUser result=success newUserId={} role={}", savedUser.getId(), savedUser.getRole());
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> {
                    securityLog.warn("ADMIN_ACTION action=updateUser result=failure reason=user_not_found targetUserId={}", id);
                    return new ResourceNotFoundException("User not found");
                });

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            securityLog.warn("ADMIN_ACTION action=updateUser result=failure reason=email_exists targetUserId={}", id);
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        var updatedUser = userRepository.save(user);
        securityLog.info("ADMIN_ACTION action=updateUser result=success targetUserId={} newRole={}", updatedUser.getId(), updatedUser.getRole());
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
