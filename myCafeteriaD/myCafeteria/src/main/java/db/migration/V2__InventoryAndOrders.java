package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Additive migration for BOTH first-time installs and historical databases
 * previously managed through Hibernate ddl-auto=update.
 *
 * MySQL 8 does not implement "ADD COLUMN IF NOT EXISTS", so use JDBC metadata
 * to keep this migration safe for already-upgraded feature-branch databases.
 * The Flyway history record still makes successful application one-time only.
 */
public class V2__InventoryAndOrders extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection db = context.getConnection();
        if (!columnExists(db, "food_menu", "stock")) {
            execute(db, "ALTER TABLE food_menu ADD COLUMN stock INTEGER NOT NULL DEFAULT 0");
        }
        if (!columnExists(db, "food_menu", "active")) {
            execute(db, "ALTER TABLE food_menu ADD COLUMN active BIT DEFAULT 1");
        }
        execute(db, "UPDATE food_menu SET stock = 0 WHERE stock IS NULL");
        execute(db, "UPDATE food_menu SET active = 1 WHERE active IS NULL");

        execute(db, "CREATE TABLE IF NOT EXISTS cafeteria_orders ("
            + "id BIGINT NOT NULL AUTO_INCREMENT, "
            + "user_id BIGINT NOT NULL, "
            + "idempotency_key VARCHAR(100) NOT NULL, "
            + "request_fingerprint VARCHAR(1500) NOT NULL, "
            + "status VARCHAR(20) NOT NULL, "
            + "total DECIMAL(12,2) NOT NULL, "
            + "created_at DATETIME(6) NOT NULL, "
            + "PRIMARY KEY(id), "
            + "CONSTRAINT uk_order_owner_key UNIQUE(user_id,idempotency_key))");

        execute(db, "CREATE TABLE IF NOT EXISTS cafeteria_order_items ("
            + "id BIGINT NOT NULL AUTO_INCREMENT, "
            + "order_id BIGINT NOT NULL, "
            + "food_id VARCHAR(255) NOT NULL, "
            + "food_name VARCHAR(255) NOT NULL, "
            + "quantity INTEGER NOT NULL, "
            + "unit_price DECIMAL(12,2) NOT NULL, "
            + "line_total DECIMAL(12,2) NOT NULL, "
            + "PRIMARY KEY(id), "
            + "CONSTRAINT fk_order_item_order FOREIGN KEY(order_id) REFERENCES cafeteria_orders(id), "
            + "CONSTRAINT fk_order_item_food FOREIGN KEY(food_id) REFERENCES food_menu(id))");
    }

    private static boolean columnExists(Connection db, String table, String column) throws SQLException {
        DatabaseMetaData meta = db.getMetaData();
        // Schema identifier casing differs between MySQL and H2 in MySQL mode.
        try (ResultSet columns = meta.getColumns(null, null, null, null)) {
            while (columns.next()) {
                if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
    private static void execute(Connection db, String sql) throws SQLException {
        try (Statement statement = db.createStatement()) {
            statement.execute(sql);
        }
    }
}
