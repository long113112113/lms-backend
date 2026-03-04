package com.example.lms_backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.example.lms_backend.dto.auth.LoginRequest;
import com.example.lms_backend.dto.auth.RegisterRequest;
import com.example.lms_backend.service.AuthService;
import com.example.lms_backend.util.CookieUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createRefreshTokenCookie(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createAccessTokenCookie(result.accessToken()).toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createRefreshTokenCookie(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createAccessTokenCookie(result.accessToken()).toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(
            @CookieValue(name = "refresh_token") String refreshToken) {
        AuthService.AuthResult result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createRefreshTokenCookie(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createAccessTokenCookie(result.accessToken()).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, CookieUtils.deleteRefreshTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, CookieUtils.deleteAccessTokenCookie().toString())
                .build();
    }
}
