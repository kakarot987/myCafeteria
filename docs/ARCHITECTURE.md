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
| M4 fulfillment | Status progression, dashboard, menu stock changes, history, atomic one-time cancellation stock restoration | Deeper admin editing UX and more operational reports |
| M5 operations | Full-stack Docker Compose, environment configuration, H2 concurrency and rollback coverage | Versioned migrations, OpenAPI and MySQL stress tests |
| M6 release | Responsive frontend, architecture documentation and sample menu | Screenshots, recorded demo, verified fresh-clone release |

## Release checklist

- [ ] Fresh clone with no local Maven/npm cache starts on supported toolchains.
- [x] CI validates Compose and starts Angular/Nginx + MySQL + API, exercising the same-origin proxy (run #42068101728).
- [x] Backend tests and Angular build pass in GitHub Actions (run #42066638181).
- [ ] Public endpoints work; anonymous admin/order requests return 401.
- [ ] Customers cannot view another customer's order (404).
- [ ] Concurrent orders do not overdraw stock or create duplicate orders.
- [ ] Failed multi-item checkout rolls back all reserved stock.
- [ ] Admin can add/remove items, modify inventory and fulfill an order.
- [ ] Production key rotation, backup/migration and security review are done.
- [ ] Screenshots and short demo are captured and linked in README.
