## 2026-03-28 — Disable SQL Query Logging in Production
**Vulnerability:** Information disclosure via SQL query logging `spring.jpa.show-sql=true` in `application.properties`.
**Root Cause:** Leftover development configuration exposing database queries and schema structure to stdout/logs in production.
**Fix Applied:** Disabled `spring.jpa.show-sql` and `spring.jpa.properties.hibernate.format_sql` in `src/main/resources/application.properties`.
**Side Effects:** None. `application-dev.properties` remains configured with `spring.jpa.show-sql=true` for local development.
**Prevention:** Keep production properties secure by default and rely on profile-specific properties (e.g., `application-dev.properties`) for debugging features.
