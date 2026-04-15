## 2026-04-15 — Information Disclosure / Unbounded Input
**Vulnerability:** Found `spring.jpa.show-sql=true` in production configuration, missing `@JsonIgnore` on the password field of the `User` entity, and an unbounded string length on `RefreshToken.deviceName`.
**Root Cause:** Security configurations left over from development (SQL logging), implicit entity serialization risks, and unbounded database column definitions.
**Fix Applied:** Disabled SQL logging in `application.properties`. Added `@JsonIgnore` to `User.password`. Added `length = 255` to `@Column` mapping for `RefreshToken.deviceName`.
**Side Effects:** None. Tested successfully.
**Prevention:** Check `.properties` to ensure DEV variables don't leak into PROD. Review all DTOs and entities for explicit limits to string inputs/storages.
