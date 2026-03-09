## 2026-03-09 — Timing Attack Mitigation in Login
**Vulnerability:** User enumeration through timing attack in `AuthService.login()`.
**Root Cause:** The `passwordEncoder.matches()` was skipped entirely if `userRepository.findByEmail()` returned empty, allowing attackers to differentiate between invalid users and valid users based on response times.
**Fix Applied:** Pre-encoded a `dummyPasswordForTimingAttackMitigation` in the `AuthService` constructor. If a user is not found, the service still performs `passwordEncoder.matches()` with the dummy hash to ensure constant response times. (File: `src/main/java/com/example/lms_backend/service/AuthService.java`)
**Side Effects:** None.
**Prevention:** Always perform constant-time comparison checks in authentication flows, regardless of whether a user is found in the database.
