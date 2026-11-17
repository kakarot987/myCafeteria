# Security merge-resolution notes

The November 2026 security PR was reconciled with a newer `main` branch that already introduced customer ordering, Flyway schema migrations, scoped CORS, role-protected legacy uploads, a Java 11 multi-stage build, and full-stack CI.

The merge **keeps the newer configuration and application behavior**. Do not restore the older MySQL 5.7 / Java 8 / Hibernate `ddl-auto=update` Compose configuration from the original feature branch.

The remaining fixes carried forward here are:
- Reject malformed uploaded food JSON with HTTP 400, without saving a partial food record.
- Return HTTP 404 for a missing legacy food image ID.
- Regression tests for customer-only signup, malformed JSON, and nonexistent food.
- Avoid logging raw uploaded food JSON.

For startup, follow the root [README](README.md), copy `myCafeteriaD/.env.example` into the ignored `myCafeteriaD/.env`, and provide fresh local credentials.

**Security note:** Removing plaintext values from the current tree does not revoke secrets exposed in old Git commits. Rotate any previously used secrets. CI and local deployments should be checked on the merged commit before making production-readiness claims.
