package com.clickart.inventoryservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clickart.inventoryservice.model.Inventory;
import com.clickart.inventoryservice.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public List<Inventory> getListOfInventory() {
        log.info("InventoryController :: getListOfInventory <--");
        log.info("InventoryController :: getListOfInventory -->");
        return inventoryService.getInventoryList();
    }

    @PostMapping("updateInventory")
    public boolean updateInventory(@RequestBody List<Inventory> request) {
        log.info("InventoryController :: updateInventory <--");
        log.info("InventoryController :: updateInventory -->");
        return inventoryService.updateInventoryStocks(request);
    }

}