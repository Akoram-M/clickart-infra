package com.clickart.inventoryservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clickart.inventoryservice.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Spring Data JPA automatically writes the SQL query: SELECT * FROM t_inventory
    // WHERE sku_code = ?
    Optional<Inventory> findBySkuCode(String skuCode);

    // Explicitly check if the row exists AND has enough stock
    @Query("SELECT COUNT(i) > 0 FROM Inventory i WHERE i.skuCode = :sku AND i.quantity >= :requestedQuantity")
    boolean hasSufficientStock(@Param("sku") String sku, @Param("requestedQuantity") Integer requestedQuantity);

    // Explicitly execute the mathematical deduction
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :value WHERE i.skuCode = :key")
    void deductStock(@Param("key") String key, @Param("value") Integer value);
}