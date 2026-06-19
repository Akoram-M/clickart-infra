package com.clickart.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// This tells the Order Service exactly how to call the Inventory Service
// We use the Docker DNS name (clickart-inventory-service) 
@FeignClient(name = "inventory-service", url = "${inventory.service.url}")
public interface InventoryClient {

    @GetMapping("/api/inventory/{skuCode}")
    boolean isInStock(@PathVariable String skuCode);
}