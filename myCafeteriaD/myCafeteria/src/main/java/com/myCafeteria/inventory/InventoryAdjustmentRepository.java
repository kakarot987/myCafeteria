package com.myCafeteria.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findTop100ByOrderByCreatedAtDescIdDesc();
    List<InventoryAdjustment> findTop100ByFoodIdOrderByCreatedAtDescIdDesc(String foodId);
}
