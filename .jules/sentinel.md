## 2026-04-16 — Disabled Production SQL Logging & Fixed Unbounded deviceName

**Vulnerability:**
1. The `spring.jpa.show-sql=true` setting in `application.properties` was enabled globally, logging sensitive SQL queries to standard out.
2. The `deviceName` column in `RefreshToken.java` did not have a defined `length` bound, exposing the database to storage DoS through arbitrarily large strings.
3. The `password` field in `User.java` was missing a serialization block (`@JsonIgnore`), posing a leakage risk.

**Root Cause:**
1. Global configuration defaults often incorrectly leave debug settings like `show-sql` enabled for development purposes without explicit profiling checks for production.
2. Hibernate creates a `VARCHAR(255)` by default, but failure to bound input length safely can lead to unoptimized storage and out-of-memory DoS risks during persistence if not constrained via `@Column(length=...)`.
3. Lack of defense-in-depth on entities makes them vulnerable to accidental JSON serialization leakage even if DTOs are correctly used right now.

**Fix Applied:**
1. Explicitly configured `spring.jpa.show-sql=false` and `spring.jpa.properties.hibernate.format_sql=false` in `application.properties`.
2. Appended `length = 255` to the `@Column` annotation over `deviceName` in `RefreshToken.java`.
3. Annotated `password` with `@com.fasterxml.jackson.annotation.JsonIgnore` inside `User.java`.

**Side Effects:** None observed. Tests still successfully passed, confirming the entity schemas remain valid for the `H2Dialect` persistence checks.

**Prevention:**
Always review base application configurations to ensure debug flags are strictly gated by development profiles. Additionally, apply strict data length limits for all String `@Column` elements representing client-provided or environment-derived values, and safeguard entity fields with `@JsonIgnore` preemptively.
