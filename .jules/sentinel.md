## 2024-05-24 — Fix unbounded VARCHAR length on RefreshToken deviceName
**Vulnerability:** Unbounded column length for `deviceName` in the `RefreshToken` entity.
**Root Cause:** The `@Column` annotation lacked the `length` property, defaulting to an unbounded VARCHAR type in PostgreSQL, leading to a potential storage DoS vulnerability.
**Fix Applied:** Added `length = 255` to the `@Column` annotation for `deviceName` in `src/main/java/com/example/lms_backend/entity/RefreshToken.java`.
**Side Effects:** N/A.
**Prevention:** Enforce adding max lengths on all String columns to prevent arbitrary memory usage.
