# Architecture and delivery checklist

## Module inventory

- `myCafeteriaAngular/src/app/homepage`: Angular UI for menu search/filter, cart, login/signup, order status, admin inventory.
- `myCafeteriaAngular/src/app/services`: auth HTTP service and browser session store.
- `myCafeteriaD/myCafeteria/src/main/java/com/myCafeteria/security`: JWT validation and BCrypt authentication.
- `controllers/AuthController`: legacy signup/signin, image and food upload endpoints.
- `foodMenu`: JPA menu and image content.
- `menu/MenuController`: admin menu and stock CRUD; public browse.
- `orders`: order entity, immutable price snapshots, idempotency, atomic stock reservation, fulfillment.
- `BootstrapData`: development roles, default menu and optional administrator initialization.
- `myCafeteriaD/docker-compose.yml`: Angular/Nginx + Spring Boot API + MySQL with a persisted database volume and a health check.

## Request architecture

Angular → `/api` proxy → Spring Security JWT filter → controller / method authorization → JPA transaction → MySQL.

Public reads: `GET /api/menu`, food images and registration. Order placement needs a valid JWT and Idempotency-Key; order ownership is determined exclusively from the authenticated principal, not from user-provided request data. Staff transitions use role-based authorization; admin inventory changes are role-restricted. Passwords are BCrypt-hashed.

## Milestone acceptance progress

| Milestone | Progress | Still needed |
| --- | --- | --- |
| M1 baseline | Module/API inventory, environment template and Docker rebuild added | Clean-room developer verification still needed; CI Docker smoke passed |
| M2 authentication | Removed signup role escalation; private order ownership, staff/admin restrictions; token flow in UI | End-to-end auth tests, token revocation/refresh, stronger signup safeguards |
| M3 ordering | Persisted order lines, stock locks, atomic totals and idempotency key | Harden concurrent retry behavior under real MySQL and validate production readiness |
| M4 fulfillment | Status progression, dashboard, full menu editing, inventory, history, one-time cancellation stock restoration | Operational reports and further UX testing |
| M5 operations | Full-stack Docker Compose, Flyway V1/V2 migrations, OpenAPI/Swagger, H2 integration tests and dedicated real-MySQL concurrency suite | Verify historical MySQL upgrade path, expand stress tests and browser E2E coverage |
| M6 release | Responsive UI, architecture documentation, seeded menu, automated browser screenshots and customer/admin E2E suite | Recorded demo, independent fresh-clone run, release tagging |

## Release checklist

- [ ] Fresh clone with no local Maven/npm cache starts on supported toolchains.
- [x] CI validates Compose and starts Angular/Nginx + MySQL + API, exercising the same-origin proxy (run #42068101728).
- [x] Backend tests and Angular build pass in GitHub Actions (run #42066638181).
- [ ] Public endpoints work; anonymous admin/order requests return 401.
- [ ] Customers cannot view another customer's order (404).
- [ ] MySQL CI concurrency tests (last-item checkout and duplicate idempotency) pass on the current commit.
- [ ] Failed multi-item checkout rolls back all reserved stock.
- [ ] Admin can add/remove items, modify inventory and fulfill an order.
- [ ] Production key rotation, historic-database backup/migration validation and security review are done.
- [ ] Verify screenshot artifact from latest browser CI and record/link a narrated short demo.

## Migration policy

Flyway V1 reproduces the legacy schema for new databases; V2 adds inventory and ordering support. Spring JPA now validates rather than updates the schema. Existing databases require explicit reviewed baselining and a backup. See [DATABASE_MIGRATIONS.md](DATABASE_MIGRATIONS.md). Do not automatically baseline any unknown production schema.

## OpenAPI and MySQL CI

Swagger UI: `/swagger-ui/index.html`; JSON: `/v3/api-docs/cafeteria`. Nginx proxies both through the frontend origin. `mysql-integration` CI runs real InnoDB transactional tests separate from the normal H2 suite, covering overlapping checkout, duplicate keys and rollback. Consult the README for the local test environment.

## Browser portfolio validation

The `e2e/` Playwright test suite covers customer/admin browser workflows and mobile layout, running after the Docker smoke test on a disposable CI database. Each successful run attaches screenshots as `cafeteria-browser-screenshots`. Local commands and caveats are in the README. Screenshot artifacts are not permanent; store reviewed release images separately before tagging.
