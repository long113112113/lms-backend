## 2024-04-05 — Prevented DB Schema Exfiltration via Logs
**Vulnerability:** `spring.jpa.show-sql=true` was enabled in `application.properties`, exposing raw SQL queries and potentially the schema structure to standard output and logs in all environments (including production).
**Root Cause:** Developer convenience during initial setup; it was not properly segregated to `application-dev.properties` or disabled for production.
**Fix Applied:** Changed `spring.jpa.show-sql=true` to `false` in `src/main/resources/application.properties`.
**Side Effects:** None.
**Prevention:** Enforce environment-specific configurations. Maintain `show-sql` true only in `application-dev.properties` and keep it explicitly disabled in `application.properties` and production profiles.

## 2024-04-05 — Prevented Password Hash Leakage on Entity Serialization
**Vulnerability:** The `User` entity lacked `@JsonIgnore` on the `password` field, risking password hash leakage if the entity is inadvertently serialized to JSON in API responses.
**Root Cause:** Pure reliance on the DTO pattern without defense in depth at the entity level.
**Fix Applied:** Added `@JsonIgnore` to `password` in `src/main/java/com/example/lms_backend/entity/User.java`.
**Side Effects:** None. API responses correctly map to DTOs.
**Prevention:** Apply defense in depth. Always add `@JsonIgnore` to sensitive fields (e.g., passwords, internal IDs, secret tokens) directly on entities, even when DTOs are used for client communication.
