package com.clickart.inventoryservice.controller;

import com.clickart.inventoryservice.model.Inventory;
import com.clickart.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    @GetMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@PathVariable String skuCode) {
        log.info("InventoryController :: isInStock <--");
        if (skuCode.equals("POISON_PILL")) {
            log.error("Intentional crash triggered for SKU: {}", skuCode);
            throw new RuntimeException("FATAL: Simulated Database Connection Dropped!");
        }
        log.info("InventoryController :: isInStock -->");
        return inventoryRepository.findBySkuCode(skuCode)
                .map(inventory -> inventory.getQuantity() > 0)
                .orElse(false);
    }

    @GetMapping
    public List<Inventory> getListOfInventory() {
        log.info("InventoryController :: getListOfInventory <--");
        log.info("InventoryController :: getListOfInventory -->");
        return inventoryRepository.findAll();
    }

    @PostMapping("updateInventory")
    public Inventory updateInventory(@RequestBody Inventory request) {
        log.info("InventoryController :: updateInventory <--");
        Inventory res = inventoryRepository.save(request);
        log.info("InventoryController :: updateInventory -->");
        return res != null ? res : null;
    }
    
}