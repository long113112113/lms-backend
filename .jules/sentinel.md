## 2024-04-07 — Fixed Device Name Unbounded Storage DoS and Minor Vulnerabilities
**Vulnerability:** `RefreshToken.deviceName` column lacked a length constraint allowing unlimited String size (unbounded storage DoS risk), `spring.jpa.show-sql=true` was active in production leading to information leakage, and `User.password` lacked `@JsonIgnore` making it susceptible to accidental exposure.
**Root Cause:** Developers relied on default `@Column` constraints which map to `varchar(255)` or `text` based on Dialect but failed to restrict entity lengths safely; development features leaked into default profiles; missing defense-in-depth on DTO patterns.
**Fix Applied:** Added `length = 255` to `@Column(name = "device_name")` in `RefreshToken.java`, set `spring.jpa.show-sql=false` in `application.properties`, and added `@JsonIgnore` to `password` in `User.java`.
**Side Effects:** None. API contracts and functionality remain intact.
**Prevention:** Always verify `@Column` annotations on `String` fields have explicit lengths. Ensure development settings stay in `-dev.properties` profiles. Implement `@JsonIgnore` proactively on sensitive fields even if DTOs are preferred.
