## 2025-04-08 — Disabled show-sql in production properties
**Vulnerability:** Information disclosure via `spring.jpa.show-sql=true` in production logs.
**Root Cause:** `spring.jpa.show-sql` was enabled globally in `application.properties` instead of being restricted to development profiles (`application-dev.properties`).
**Fix Applied:** Set `spring.jpa.show-sql=false` and `spring.jpa.properties.hibernate.format_sql=false` in `src/main/resources/application.properties`.
**Side Effects:** None.
**Prevention:** Always verify that verbose logging flags are disabled in production configurations.
