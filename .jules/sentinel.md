## 2026-04-11 — Disabled SQL Logging in Production
**Vulnerability:** SQL queries and schema structure were being leaked to stdout/logs in production due to `spring.jpa.show-sql=true` and `spring.jpa.properties.hibernate.format_sql=true` in `application.properties`.
**Root Cause:** The properties were likely left enabled during initial development to help debug JPA queries, but not reverted or isolated to a dev profile.
**Fix Applied:** Updated `src/main/resources/application.properties` to set `spring.jpa.show-sql=false` and `spring.jpa.properties.hibernate.format_sql=false` and included a `// SECURITY:` comment.
**Side Effects:** No functional side effects. It reduces noise in production logs and stops information disclosure.
**Prevention:** In Spring Boot projects, strictly review `application.properties` (especially for `spring.jpa.show-sql` and `management.endpoints.web.exposure.include`) to ensure secure-by-default behavior, keeping such settings disabled or restricted in the base or production profiles.
