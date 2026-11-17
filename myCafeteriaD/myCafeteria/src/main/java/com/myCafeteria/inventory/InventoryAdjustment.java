package com.myCafeteria.inventory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.persistence.*;

/** A point-in-time inventory movement; rows should never be edited after insertion. */
@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "food_id", nullable = false)
    private String foodId;
    @Column(name = "food_name", nullable = false)
    private String foodName;
    @Column(name = "actor_id")
    private Long actorId;
    @Column(name = "order_id")
    private Long orderId;
    @Column(nullable = false, length = 40)
    private String reason;
    @Column(nullable = false)
    private int delta;
    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    protected InventoryAdjustment() {}

    public InventoryAdjustment(String foodId, String foodName, Long actorId, Long orderId,
                               String reason, int delta, int quantityAfter) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.actorId = actorId;
        this.orderId = orderId;
        this.reason = reason;
        this.delta = delta;
        this.quantityAfter = quantityAfter;
    }
    public Long getId() { return id; }
    public String getFoodId() { return foodId; }
    public String getFoodName() { return foodName; }
    public Long getActorId() { return actorId; }
    public Long getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public int getDelta() { return delta; }
    public int getQuantityAfter() { return quantityAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
