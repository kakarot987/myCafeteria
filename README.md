# myCafeteria ☕

A cafeteria ordering application with an Angular customer experience, a Java Spring Boot REST API, and a MySQL database. The customer can browse the menu, manage a tray, place orders, and follow progress. Staff can move orders through fulfillment, while administrators manage menu stock.

> **Status:** The original repository uses Angular 11, Spring Boot 2.4 and a legacy schema. This branch is a recovery and feature enhancement. Run the smoke tests and CI before production use. Legacy dependency upgrades and historical-database upgrade validation are tracked separately.

## Quick start (one command for the full stack)

Requirements: Docker Engine with Docker Compose v2. **No local Node, npm, Java, or MySQL installation is required** when using Docker.

1. Clone this branch and enter the repository directory.
2. Copy `myCafeteriaD/.env.example` to `myCafeteriaD/.env` and replace all placeholder credentials with **unique random values** (JWT key: 64+ characters; bootstrap admin password: 12+ characters). Running `./start.sh` without a file creates the template and asks you to edit it.
3. Run **`./start.sh`** from the repository root. This builds and starts MySQL, Spring Boot, and Angular/Nginx together.
4. Open **http://localhost:4200** for the frontend. The same-origin Nginx proxy forwards `/api/` to Spring Boot (also exposed at http://localhost:8080 for debugging).
5. Sign in with the bootstrap administrator credentials from the environment file. Register a separate customer account through the form to test checkout.

To stop all services: `./start.sh down`. To follow container logs: `./start.sh logs`.

On Windows without a POSIX shell, run these commands from the repository root after creating `myCafeteriaD/.env`:

```powershell
docker compose --env-file myCafeteriaD/.env -f myCafeteriaD/docker-compose.yml up -d --build
```

The database uses a persistent Docker volume. **Do not** run `docker compose down -v` unless you intend to permanently erase the data. First-run initialization creates seven sample dishes and standard roles. The bootstrap admin is created only when the username is absent. Remove the bootstrap credentials from your environment after initial setup.

### Optional: run the frontend outside Docker

For frontend hot reload, start only the database and API with `cd myCafeteriaD && docker compose up -d --build mysqldb app`, then run `cd myCafeteriaAngular && npm ci && npm start` using Node 14. The Angular development proxy maps `/api` to localhost:8080. Avoid running the Docker frontend simultaneously on port 4200.

## Frontend

`myCafeteriaAngular/` uses Angular 11 with responsive menu cards, category chips, search, session-based JWT authentication, a tray with quantities, order history, a fulfillment view, and an inventory dashboard. The Docker frontend uses a multi-stage Angular build and serves the generated assets with Nginx; its same-origin `/api/` reverse proxy routes to the `app` service. For development outside Docker, `proxy.conf.json` routes Angular CLI requests to `localhost:8080`.

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

Order transition flow: `PLACED → PREPARING → READY → COMPLETED`; orders can be cancelled from `PLACED` or `PREPARING` by staff. Cancelled orders restore their stock once, within the same database transaction. Invalid transitions or attempts to cancel the same order again return HTTP 409.

## Admin analytics, customer experience and inventory controls

The **Dashboard** tab (administrator role) provides:
- Completed-order value, completed and placed counts, average kitchen prep time, and status breakdown.
- **Daily / ISO-week sales charts** and 7-/30-day reports, calculated server-side using UTC dates.
- Top five dishes by quantity across completed orders, and a configurable low-stock threshold (default 5).
- Shortcuts to the kitchen queue and inventory management.

**Accounting note:** the `revenue` API field represents **gross completed-order value**, not reconciled payments or net revenue. The app still collects payment at pickup; no payment capture, refunds, tax accounting or cash reconciliation are implemented. Cancelled/unfulfilled orders are excluded. Older orders without completion timestamps are not included in completion-date reporting until they have valid timestamps.

The **Kitchen orders** view now supports active/all/status filters, preparation elapsed time, guarded fulfillment transitions and 15-second refreshes while the view is visible. Customer orders show a visible **Placed → Preparing → Ready → Completed** timeline, pickup confirmation, and automatic status refresh. A successful checkout displays an order confirmation; loading, empty and network-error states are clearer.

The customer's cart is now saved in local browser storage under an account-specific key. This is convenience persistence, **not** authoritative pricing or inventory: checkout always uses server prices and validates stock. Cart quantities survive page reload, unavailable menu items are discarded on refresh, and the idempotency key remains available for a retry after an uncertain network response. Stored carts are device-local and can be cleared from browser storage.

The administrator **Inventory** view filters all, low-stock, sold-out and category items, and shows the last 100 inventory movements. Stock audits record administrator create/edit/stock/deactivation and checkout stock reservations/cancellations, with the operation's final stock quantity. Audit records are written transactionally alongside the inventory changes.

### New administrator endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/admin/overview?days=7&lowStockThreshold=5` | Daily/weekly operational reports, popular dishes, stock alerts |
| GET | `/api/admin/inventory/history` | Latest 100 inventory events |
| GET | `/api/admin/inventory/history?foodId=ID` | Latest 100 events for a specific dish |

Both endpoints require the administrator role. Their OpenAPI definitions appear in `/v3/api-docs/cafeteria`.

## Interactive API documentation

Swagger UI: **http://localhost:4200/swagger-ui/index.html** (or directly through the backend at `http://localhost:8080/swagger-ui/index.html`).
The machine-readable OpenAPI 3 document for supported customer and admin routes is at
**http://localhost:4200/v3/api-docs/cafeteria**.

Use `POST /api/auth/signin` to get a JWT, choose **Authorize** in Swagger UI, and enter the token.
The documented `POST /api/orders` endpoint also requires the `Idempotency-Key` header: use a unique value for each new checkout, retaining it only for retries of the same cart. Never use real account credentials in public screenshots or published recordings.

The documented authentication and menu browse routes are public; customer/admin operations remain protected even though their API descriptions are public. Legacy file/upload and test routes are intentionally excluded from the focused `cafeteria` API group.

## MySQL transaction tests

The CI workflow runs a dedicated MySQL 8 job (`mysql-integration`) that tests two simultaneous buyers competing for one item, two simultaneous retries using the same idempotency key, and rollback when one item in a multi-item checkout is invalid. These tests use genuine InnoDB locks and Flyway migrations, rather than the faster H2 test database.

To run locally, start a disposable MySQL 8 database named `cafeteria_it` with a dedicated test user, then set `RUN_MYSQL_IT=true`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and a test-only `APP_JWT_SECRET`. From the backend directory run:

```bash
mvn -B -Dtest=MySqlConcurrencyIT test
```

These tests create and change real database rows. **Never point them at a valuable or production database.**

## Portfolio release and browser walkthrough

The **Playwright Chromium** suite in `e2e/` verifies the workflow in a real browser:
administrator login → menu item creation and editing → customer registration and checkout → customer order ownership → staff order fulfillment → customer status refresh. A separate mobile viewport test checks navigation and horizontal overflow.

The GitHub Actions `docker-smoke` job starts the full three-service stack and runs the browser tests against `http://127.0.0.1:4200`. The job uploads a **cafeteria-browser-screenshots** artifact containing desktop storefront/cart, admin inventory, order tracking, and mobile screenshots. In GitHub, open the latest successful workflow run and find the artifact under **Artifacts**. Screenshots are generated from disposable CI accounts; no sample credentials or recorded tokens should be published.

For a local browser run, start the application with `./start.sh` using nonproduction accounts, then in a second terminal:

```bash
cd e2e
npm install
npx playwright install chromium
E2E_ADMIN_USER=admin E2E_ADMIN_PASSWORD='your-local-bootstrap-password' npm test
```

These browser tests create and update data in the configured database. Use a disposable environment. The browser suite requires Node 18+ separately from the legacy Angular 11 build, but the application itself needs only Docker when using `./start.sh`.

A recorded narrated demo and independent clean-room upgrade of a historical database are still release checklist items; browser screenshots do not substitute for these.

## Verification

- Backend: `cd myCafeteriaD/myCafeteria && ./mvnw test` (Java 11; access to Maven Central required).
- Frontend: `cd myCafeteriaAngular && npm ci && npm run build`.
- Docker full stack: `./start.sh`, then open `http://localhost:4200` and verify `http://localhost:4200/api/menu` returns the sample menu.
- Manual workflow: login → browse menu → add dishes → checkout → order history → admin/staff status updates → customer refresh → stock changes.
- Repeat a checkout with the same Idempotency-Key; verify a second order is not created. Try an unauthorized admin action and another user's order URL.

## Security and migration considerations

The historic Git history contains a previously committed `.env` and application configuration with hard-coded secrets. The working-tree version removes the committed `.env` and example values are placeholders, **but Git history remains**: rotate any previously used database credentials and JWT signing keys. Rewrite history with a coordinated migration if required.

The existing Spring Boot version is end-of-life and the Angular toolchain is old. Plan upgrades before exposing the application publicly. **Flyway now owns the database schema** with V1 and V2 migrations; Hibernate uses `ddl-auto=validate`. Fresh Docker installs run both migrations automatically. **Existing pre-Flyway databases require an explicit baseline and backup**: follow [the migration guide](docs/DATABASE_MIGRATIONS.md) before attempting any upgrade. Do not point this branch at a production database without a tested backup and schema review.

See [architecture & release checklist](docs/ARCHITECTURE.md) and the GitHub Issues for remaining work.
