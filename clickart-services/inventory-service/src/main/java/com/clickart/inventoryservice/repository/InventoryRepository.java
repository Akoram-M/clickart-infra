package com.clickart.inventoryservice.repository;

import com.clickart.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    // Spring Data JPA automatically writes the SQL query: SELECT * FROM t_inventory WHERE sku_code = ?
    Optional<Inventory> findBySkuCode(String skuCode);
}