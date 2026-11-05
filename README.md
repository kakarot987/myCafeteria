# myCafeteria ☕

A cafeteria ordering application with an Angular customer experience, a Java Spring Boot REST API, and a MySQL database. The customer can browse the menu, manage a tray, place orders, and follow progress. Staff can move orders through fulfillment, while administrators manage menu stock.

> **Status:** The original repository uses Angular 11, Spring Boot 2.4 and a legacy schema. This branch is a recovery and feature enhancement. Run the smoke tests and CI before production use. Legacy dependency upgrades and database migrations are tracked separately.

## Quick start (Docker + Node)

Requirements: Docker Engine + Docker Compose v2; Node 14 (recommended for Angular CLI 11) and npm.

1. Clone: `git clone https://github.com/kakarot987/myCafeteria.git` and enter the directory.
2. Copy `myCafeteriaD/.env.example` to `myCafeteriaD/.env`. Replace **all** placeholder secrets with unique random values (JWT secret: 64+ characters; admin password: 12+ characters).
3. Start the backend: `cd myCafeteriaD && docker compose up --build -d` (API: `http://localhost:8080`).
4. Start the UI in another terminal: `cd myCafeteriaAngular && npm ci && npm start`. Open `http://localhost:4200`.
5. Sign in with the bootstrap administrator credentials from the environment file. For customer testing, create a customer account through the sign-up form and log in.

The database is persisted in a Docker named volume. For a fresh reset run `docker compose down -v` **only if you are happy to delete all database data**. First-run data populates seven sample dishes and the standard roles. The admin account is created only if its username does not exist. Remove bootstrap admin credentials after initial setup.

## Frontend

`myCafeteriaAngular/` uses Angular 11 with responsive menu cards, category chips, search, session-based JWT authentication, a tray with quantities, order history, a fulfillment view, and an inventory dashboard. Angular dev-server proxies `/api` to `localhost:8080` using `proxy.conf.json`. For a production frontend, configure your web server/reverse proxy to forward `/api` requests to the backend (and serve the compiled Angular assets).

## Backend

`myCafeteriaD/myCafeteria/` uses Spring Boot 2.4, Java 11, Spring Security JWT, JPA and MySQL 8. The code stores only BCrypt password hashes. Authentication is stateless, and the frontend keeps the JWT in browser session storage. For production, evaluate HttpOnly cookie-based auth, refresh/revocation, CSRF controls and TLS.

### API flow

| Method | Path | Who | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | Public | Customer registration; requested roles are ignored |
| POST | `/api/auth/signin` | Public | Sign in and receive JWT |
| GET | `/api/menu` | Public | Browse active menu and stock |
| POST | `/api/menu` | Admin | Create menu item |
| PUT | `/api/menu/{id}` | Admin | Edit name, description, category, price and stock |
| PATCH | `/api/menu/{id}/stock` | Admin | Adjust stock |
| DELETE | `/api/menu/{id}` | Admin | Soft-delete menu item |
| POST | `/api/orders` | Logged-in customer | Place an atomic order; requires `Idempotency-Key` header |
| GET | `/api/orders/mine` | Logged in | View own order history |
| GET | `/api/orders/{id}` | Owner/staff | View one order; non-owner receives 404 |
| GET | `/api/orders` | Staff/admin | Fulfillment queue |
| PATCH | `/api/orders/{id}/status` | Staff/admin | Advance order state |
| GET | `/api/auth/getFood` | Public | Legacy food list |
| GET | `/api/auth/getFood/{id}` | Public | Legacy food image |
| POST | `/api/auth/uploadfood` | Admin | Legacy food upload |

Other legacy profile/upload endpoints require an authenticated/admin identity as enforced by Spring Security and method authorization.

**Example checkout** (authenticated customer):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: my-unique-order-key" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"foodId":"REPLACE_WITH_MENU_ID","quantity":2}]}'
```

The server reads price from the database, validates quantities and availability, uses database row locks to reserve stock, and commits the order and lines in a transaction. A duplicate key for the same user/cart returns the original order. Reusing the key with a different cart returns HTTP 409. Concurrent duplicate submissions may return a conflict and should be reconciled with order history. Invalid stock/price rolls back all changes.

Order transition flow: `PLACED → PREPARING → READY → COMPLETED`; orders can be cancelled from `PLACED` or `PREPARING` by staff. Invalid transitions return HTTP 409. Cancellation currently does not restock automatically.

## Verification

- Backend: `cd myCafeteriaD/myCafeteria && ./mvnw test` (Java 11; access to Maven Central required).
- Frontend: `cd myCafeteriaAngular && npm ci && npm run build`.
- Docker: `cd myCafeteriaD && docker compose config && docker compose up --build`.
- Manual workflow: login → browse menu → add dishes → checkout → order history → admin/staff status updates → customer refresh → stock changes.
- Repeat a checkout with the same Idempotency-Key; verify a second order is not created. Try an unauthorized admin action and another user's order URL.

## Security and migration considerations

The historic Git history contains a previously committed `.env` and application configuration with hard-coded secrets. The working-tree version removes the committed `.env` and example values are placeholders, **but Git history remains**: rotate any previously used database credentials and JWT signing keys. Rewrite history with a coordinated migration if required.

The existing Spring Boot version is end-of-life and the Angular toolchain is old. Plan upgrades before exposing the application publicly. JPA currently uses `ddl-auto=update` only for development compatibility; moving to explicit versioned migrations needs a tested migration from the historical database schema. Do not point this branch at a production database without a backup.

See [architecture & release checklist](docs/ARCHITECTURE.md) and the GitHub Issues for remaining work.
