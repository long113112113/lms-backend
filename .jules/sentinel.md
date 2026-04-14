## 2026-04-14 — Mitigate Timing Attacks in AuthService
**Vulnerability:** User enumeration timing attack in `AuthService.java`
**Root Cause:** If a user was not found by email, it immediately threw a `BadCredentialsException` without checking the password hash. This created a noticeable timing difference compared to when the user was found and the password hash was verified.
**Fix Applied:** Generated a dummy hash in the constructor and used `passwordEncoder.matches()` against the dummy hash when a user is not found to ensure constant-time response and prevent user enumeration. See `src/main/java/com/example/lms_backend/service/AuthService.java`.
**Side Effects:** None.
**Prevention:** Always verify passwords even if a user is not found to ensure constant-time responses in authentication flows.
