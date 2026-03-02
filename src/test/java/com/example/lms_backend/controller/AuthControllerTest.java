package com.example.lms_backend.controller;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.lms_backend.config.SecurityConfig;
import com.example.lms_backend.dto.auth.LoginRequest;
import com.example.lms_backend.exception.GlobalExceptionHandler;
import com.example.lms_backend.exception.BadCredentialsException;
import com.example.lms_backend.service.AuthService;
import com.example.lms_backend.service.AuthService.AuthResult;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // ──────────────────────────────────────────────
    // Mocks: JWT with roles
    // ──────────────────────────────────────────────
    private static final UUID USER_ID = UUID.randomUUID();

    private static RequestPostProcessor authJwt() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()).claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }

    private AuthResult sampleAuthResult() {
        return new AuthResult("mock-access-token", "mock-refresh-token");
    }

    // ═══════════════════════════════════════════════
    // 1. POST /api/auth/login
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {
        private static final String URL = "/api/auth/login";

        private final String VALID_BODY = """
                {
                    "email": "test@example.com",
                    "password": "Password123!",
                    "deviceId": "DEVICE-01"
                }
                """;

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("400 - XSS Injection in Email/DeviceId")
        void shouldReturn400_WhenXssInFields() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "<script>alert(1)</script>@test.com",
                                "password": "Password123!",
                                "deviceId": "<img src=x onerror=alert('hacked')>"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.deviceId").exists());
        }

        @Test
        @DisplayName("400 - Malformed JSON")
        void shouldReturn400_WhenMalformedJson() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "email": "test@example.com",
                                "password": "123
                            """)) // Missing closing braces/quotes
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid data type"));
        }

        @Test
        @DisplayName("400 - Buffer Overflow (Email > 100 chars)")
        void shouldReturn400_WhenBufferOverflow() throws Exception {
            String longEmail = "a".repeat(100) + "@test.com";
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "email": "%s",
                                "password": "Password123!",
                                "deviceId": "DEVICE-01"
                            }
                            """, longEmail)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(get(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isMethodNotAllowed()); // Requires POST. GET returns 405 because permitAll
                                                               // allows it through security filter
        }

        // ── Validation & Business Logic ──

        @Test
        @DisplayName("400 - Validation error (Missing fields)")
        void shouldReturn400_WhenValidationFails() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.password").exists())
                    .andExpect(jsonPath("$.errors.deviceId").exists());
        }

        @Test
        @DisplayName("401 - Invalid credentials (Wrong Email/Password)")
        void shouldReturn401_WhenInvalidCredentials() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid email or password"));

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("200 - Successful Login")
        void shouldReturn200_WhenLoginSuccess() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResult());

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }
    }

    // ═══════════════════════════════════════════════
    // 2. POST /api/auth/refresh
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshToken {
        private static final String URL = "/api/auth/refresh";
        private static final String COOKIE_NAME = "refresh_token";

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("400 - Missing Cookie (Empty request)")
        void shouldReturn400_WhenMissingCookie() throws Exception {
            mockMvc.perform(post(URL))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Required cookie 'refresh_token' for method parameter type String is not present")); // Handled
                                                                                                                        // by
                                                                                                                        // MissingRequestCookieException
        }

        @Test
        @DisplayName("405 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            mockMvc.perform(get(URL)
                    .cookie(new Cookie(COOKIE_NAME, "mock-refresh-token")))
                    .andExpect(status().isMethodNotAllowed());
        }

        // ── Validation & Business Logic ──

        @Test
        @DisplayName("401 - Invalid/Expired Refresh Token")
        void shouldReturn401_WhenInvalidToken() throws Exception {
            when(authService.refreshToken(anyString()))
                    .thenThrow(new BadCredentialsException("Invalid refresh token"));

            mockMvc.perform(post(URL)
                    .cookie(new Cookie(COOKIE_NAME, "invalid-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid refresh token"));
        }

        @Test
        @DisplayName("200 - Successful Refresh")
        void shouldReturn200_WhenRefreshSuccess() throws Exception {
            when(authService.refreshToken(anyString())).thenReturn(sampleAuthResult());

            mockMvc.perform(post(URL)
                    .cookie(new Cookie(COOKIE_NAME, "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }
    }

    // ═══════════════════════════════════════════════
    // 3. POST /api/auth/logout
    // ═══════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {
        private static final String URL = "/api/auth/logout";

        private final String VALID_BODY = """
                {
                    "deviceId": "DEVICE-01"
                }
                """;

        // ── Security & Malicious Payloads ──

        @Test
        @DisplayName("401 - Unauthenticated request")
        void shouldReturn401_WhenUnauthenticated() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isUnauthorized()); // Requires user context
        }

        @Test
        @DisplayName("400 - XSS Injection in DeviceId")
        void shouldReturn400_WhenXssInDeviceId() throws Exception {
            mockMvc.perform(post(URL).with(authJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "deviceId": "<script>alert(1)</script>"
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.deviceId").exists());
        }

        @Test
        @DisplayName("400 - Buffer Overflow in DeviceId (> 100 chars)")
        void shouldReturn400_WhenBufferOverflow() throws Exception {
            String longDeviceId = "A".repeat(150);
            mockMvc.perform(post(URL).with(authJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(String.format("""
                            {
                                "deviceId": "%s"
                            }
                            """, longDeviceId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.deviceId").exists());
        }

        @Test
        @DisplayName("400 - Wrong HTTP Method (GET instead of POST)")
        void shouldReturn405_WhenWrongMethod() throws Exception {
            // Because AuthController expects auth context but the method is wrong
            // Without token it could be 401. Let's try it with token.
            mockMvc.perform(get(URL).with(authJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isMethodNotAllowed()); // Usually springs to 405 because GET doesn't map, and
                                                               // auth passes.
        }

        // ── Validation & Business Logic ──

        @Test
        @DisplayName("400 - Validation error (Missing fields)")
        void shouldReturn400_WhenValidationFails() throws Exception {
            mockMvc.perform(post(URL).with(authJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.deviceId").exists());
        }

        @Test
        @DisplayName("204 - Successful Logout")
        void shouldReturn204_WhenLogoutSuccess() throws Exception {
            doNothing().when(authService).logout(eq(USER_ID), anyString());

            mockMvc.perform(post(URL).with(authJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().value("refresh_token", "")); // Cookie cleared
        }
    }
}
