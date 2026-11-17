# Security and local setup

Public signup no longer accepts caller-assigned moderator/admin privileges. Protected endpoints require a valid JWT; role provisioning must happen in trusted server-side administration. Food JSON parsing now rejects malformed input rather than saving incomplete records; missing food IDs return 404.

Copy `myCafeteriaD/.env.example` to `myCafeteriaD/.env`, replace the placeholders with unique strong values, then run `docker compose --env-file .env -f docker-compose.yml config` from inside `myCafeteriaD/`. Compose binds the application to loopback and does not expose MySQL publicly. Do not deploy on the public internet without updating this legacy application's dependencies and access controls.

A tracked `.env` file had previously contained a database password. Its tracked contents are neutralized, but original secrets remain in Git history; rotate all reused credentials. Do not mistake changing a tracked placeholder for revoking an existing credential. CI and local startup still need verification.
