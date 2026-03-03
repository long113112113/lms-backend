
## 2023-10-27 - Unbounded Device Name and SQL Logging Exposure
**Vulnerability:** The `RefreshToken` entity had an unbounded `deviceName` string column allowing storage DoS. Additionally, `spring.jpa.show-sql=true` was enabled globally, exposing SQL queries to logs in production.
**Root Cause:** Missing explicit length limit on string properties resulting in potentially unbounded storage fields, and retaining development configurations (show-sql) in production configuration files.
**Fix Applied:** Added `@Column(name = "device_name", length = 255)` to `RefreshToken.deviceName` and set `spring.jpa.show-sql=false` in `application.properties`.
**Side Effects:** Disabling `show-sql` may hinder local debugging unless re-enabled locally.
**Prevention:** Always enforce `@Column(length=...)` explicitly on all String entity fields unless unbounded text is intentionally required. Isolate development logging configurations (`show-sql`) to a dedicated `application-dev.properties` profile to avoid leaking information in production.
