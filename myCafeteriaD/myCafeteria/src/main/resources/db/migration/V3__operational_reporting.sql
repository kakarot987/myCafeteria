-- Order lifecycle timing (nullable so historical orders remain valid).
ALTER TABLE cafeteria_orders ADD COLUMN started_at DATETIME(6);
ALTER TABLE cafeteria_orders ADD COLUMN ready_at DATETIME(6);
ALTER TABLE cafeteria_orders ADD COLUMN completed_at DATETIME(6);
ALTER TABLE cafeteria_orders ADD COLUMN cancelled_at DATETIME(6);

-- Auditable stock movements, committed in the SAME transaction as their cause.
CREATE TABLE inventory_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    food_id VARCHAR(255) NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    actor_id BIGINT,
    order_id BIGINT,
    reason VARCHAR(40) NOT NULL,
    delta INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_inventory_food_time ON inventory_adjustments (food_id, created_at);
CREATE INDEX idx_inventory_created ON inventory_adjustments (created_at);
