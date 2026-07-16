package com.clickart.inventoryservice.controller;

import com.clickart.inventoryservice.model.Inventory;
import com.clickart.inventoryservice.repository.InventoryRepository;
import com.clickart.inventoryservice.service.InventoryService;

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
    private final InventoryService inventoryService;

    @GetMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@PathVariable String skuCode) {
        log.info("InventoryController :: isInStock <--");
        log.error("Intentional crash triggered for SKU: {}", skuCode);

        log.info("InventoryController :: isInStock -->");
        return inventoryService.checkProductIsInStock(skuCode);
    }

    @GetMapping
    public List<Inventory> getListOfInventory() {
        log.info("InventoryController :: getListOfInventory <--");
        log.info("InventoryController :: getListOfInventory -->");
        return inventoryService.getInventoryList();
    }

    @PostMapping("updateInventory")
    public boolean updateInventory(@RequestBody List<Inventory> request) {
        log.info("InventoryController :: updateInventory <--");
        // List<Inventory> res = inventoryRepository.saveAll(request);
        log.info("InventoryController :: updateInventory -->");
        // return res != null ? true : false;
        return inventoryService.updateInventoryStocks(request);
    }

}