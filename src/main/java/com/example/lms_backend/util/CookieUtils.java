package com.example.lms_backend.util;

import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";
    private static final long REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60;

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String ACCESS_TOKEN_PATH = "/";
    private static final long ACCESS_TOKEN_MAX_AGE = 15 * 60;

    private CookieUtils() {
    }

    public static ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .build();
    }

    public static ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(0)
                .build();
    }

    public static ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(ACCESS_TOKEN_PATH)
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .build();
    }

    public static ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(ACCESS_TOKEN_PATH)
                .maxAge(0)
                .build();
    }
}
