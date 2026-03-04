package com.example.lms_backend.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms_backend.dto.user.CreateUserWithRoleRequest;
import com.example.lms_backend.dto.user.UpdateUserRequest;
import com.example.lms_backend.dto.user.UserResponse;
import com.example.lms_backend.entity.enums.Role;
import com.example.lms_backend.service.UserService;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        int maxPageSize = 100;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = PageRequest.of(pageable.getPageNumber(), maxPageSize,
                    pageable.getSort());
        }
        return ResponseEntity.ok(userService.getAllUsers(email, fullName, role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUserWithRole(
            @Valid @RequestBody CreateUserWithRoleRequest request) {
        var newUser = userService.createUserWithRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        var updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }
}
