## 2024-05-24 — Disable show-sql in Production Configuration
**Vulnerability:** Information Disclosure (CWE-200). `spring.jpa.show-sql` was set to `true` in `application.properties`.
**Root Cause:** The property was left enabled by default for development convenience without being isolated to `application-dev.properties` or overridden for production.
**Fix Applied:** Changed `spring.jpa.show-sql=true` to `spring.jpa.show-sql=false` in `src/main/resources/application.properties`. It remains `true` in `application-dev.properties`.
**Side Effects:** SQL queries will no longer be visible in the logs for the default/production profiles. This is desired.
**Prevention:** Always maintain a split configuration where debug flags (like show-sql, trace logs, or H2 consoles) are strictly limited to `application-dev.properties` or active only when explicitly enabled by a flag (e.g. `spring.profiles.active=dev`).
