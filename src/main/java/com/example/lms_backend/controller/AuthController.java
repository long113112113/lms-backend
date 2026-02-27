package com.example.lms_backend.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lms_backend.dto.auth.LoginRequest;
import com.example.lms_backend.dto.auth.LogoutRequest;
import com.example.lms_backend.dto.auth.TokenResponse;
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
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createRefreshTokenCookie(result.refreshToken()).toString())
                .body(new TokenResponse(result.accessToken(), "Bearer", 15 * 60));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @CookieValue(name = "refresh_token") String refreshToken) {
        AuthService.AuthResult result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, CookieUtils.createRefreshTokenCookie(result.refreshToken()).toString())
                .body(new TokenResponse(result.accessToken(), "Bearer", 15 * 60));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LogoutRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        authService.logout(userId, request.deviceId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, CookieUtils.deleteRefreshTokenCookie().toString())
                .build();
    }
}
