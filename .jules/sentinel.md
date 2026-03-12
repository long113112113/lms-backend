
## 2024-03-12 — Fixed High-Priority Security Issues in RefreshToken and application.properties
**Vulnerability:**
1. `RefreshToken.deviceName` lacked a `@Column(length=...)` constraint, allowing unbounded input and potential Storage DoS.
2. `spring.jpa.show-sql=true` was enabled in `application.properties`, leaking schema and queries to production logs.
**Root Cause:**
1. Default JPA string column generation without explicit length limits.
2. Development settings accidentally left in the main application properties file used for production.
**Fix Applied:**
1. Added `length = 255` to `@Column(name = "device_name")` in `src/main/java/com/example/lms_backend/entity/RefreshToken.java`.
2. Disabled `spring.jpa.show-sql` and `spring.jpa.properties.hibernate.format_sql` in `src/main/resources/application.properties` and ensured they are enabled in `application-dev.properties`.
**Side Effects:** None. Tests passed.
**Prevention:**
1. Always specify `length` for String columns in JPA entities.
2. Separate development-only configurations into profile-specific files like `application-dev.properties` and enforce strict defaults in `application.properties`.
