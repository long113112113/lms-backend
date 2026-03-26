## 2024-11-20 — Unbounded RefreshToken.deviceName Storage
**Vulnerability:** The `deviceName` field in `RefreshToken.java` lacked a `length` attribute in its `@Column` annotation, defaulting to an unlimited `VARCHAR` in PostgreSQL.
**Root Cause:** Missing explicit length constraint on a string field mapped to a database column.
**Fix Applied:** Added `length = 255` to the `@Column(name = "device_name")` annotation in `src/main/java/com/example/lms_backend/entity/RefreshToken.java`.
**Side Effects:** None.
**Prevention:** Always specify a `length` attribute for `@Column` annotations on `String` fields to bound input and prevent storage DoS.
