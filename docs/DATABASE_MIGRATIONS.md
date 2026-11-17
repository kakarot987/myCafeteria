# Database migrations

This project now uses **Flyway** for versioned database schema changes. Hibernate runs with `ddl-auto=validate`, not `update`; unexpected schema changes will fail startup instead of silently modifying tables.

## Fresh databases

The normal `./start.sh` workflow starts an empty MySQL database. Flyway applies:

- `V1__legacy_schema.sql`: original user, role, menu, image, and profile tables.
- `V2__InventoryAndOrders.java`: adds inventory fields, itemized orders, price snapshots and uniqueness constraints. Existing inventory columns and order tables are preserved if found, for users who already ran the prior feature branch with Hibernate auto-update.

The sample menu is seeded by `BootstrapData` only when there are no menu rows.

## Upgrading an existing pre-Flyway database

**This process has not been validated against a historical production database dump. Test against a restored copy first.**

1. **Stop all writers and back up the database.** For example, use `mysqldump --single-transaction` on MySQL 8 and verify that the dump can be restored.
2. Inspect the actual table names and columns, especially `food_menu`, `roles`, `users`, `user_roles`, `files`, `user_pic`, `cafeteria_orders` and `cafeteria_order_items`. Compare to `V1__legacy_schema.sql`. Resolve any schema drift or missing tables *before* proceeding.
3. For an older schema known to match the V1 baseline, temporarily set `APP_FLYWAY_BASELINE_ON_MIGRATE=true` in `myCafeteriaD/.env`. Flyway records baseline version 1 without rerunning the old CREATE TABLE statements; migration V2 then applies additive changes. **Do not enable this indiscriminately for an arbitrary existing database**.
4. Start the application and check `flyway_schema_history` for baseline 1 and successful migration 2. Verify user accounts, menu data, orders, and stock.
5. Set `APP_FLYWAY_BASELINE_ON_MIGRATE=false` again and restart; Flyway will continue using schema history.
6. Record a restore procedure and verify a rollback by restoring the pre-migration backup into an isolated environment.

**Important:** This migration initializes stock on pre-existing menu rows to **zero** where it did not previously exist, preventing accidental overselling. Administrators should explicitly set stock levels after upgrade.

## Change policy

New schema modifications should go into a new `V3__*.sql` or `V3__*.java` migration. Do not edit previously applied migration versions or checksums. Run the backend integration tests and the Docker smoke test for every migration. Never use `flyway clean` on a database with valuable data.

Flyway supports validation and repair commands; only run repair after investigation, backup, and explicit approval because it edits schema history.
