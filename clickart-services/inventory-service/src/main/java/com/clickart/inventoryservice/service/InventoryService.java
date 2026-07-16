package com.clickart.inventoryservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clickart.inventoryservice.dto.OrderPlacedEvent;
import com.clickart.inventoryservice.model.Inventory;
import com.clickart.inventoryservice.repository.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper;
    public boolean checkProductIsInStock(String skuCode) {
        return inventoryRepository.findBySkuCode(skuCode).map(inventory -> inventory.getQuantity() > 0).orElse(false);
    }

    public List<Inventory> getInventoryList() {
        return inventoryRepository.findAll();
    }

    public boolean updateInventoryStocks(List<Inventory> request) {
        List<Inventory> res = inventoryRepository.saveAll(request);
        return res != null ? true : false;
    }

    @KafkaListener(topics = "order-placed-topic", groupId = "inventory-group")
    @Transactional
    public void deductStock(String eventJson) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(eventJson, OrderPlacedEvent.class);
            log.info("Deducting inventory for Order: {}", event.getOrderNumber());

            for (Map.Entry<String, Integer> entry : event.getSkuQuantities().entrySet()) {
                String sku = entry.getKey();
                Integer quantityToDeduct = entry.getValue();

                Inventory inventory = inventoryRepository.findBySkuCode(sku)
                        .orElseThrow(() -> new RuntimeException("SKU not found in inventory: " + sku));

                int newQuantity = inventory.getQuantity() - quantityToDeduct;
                if (newQuantity < 0) {
                    log.error("CRITICAL: Negative stock reached for SKU {}. Manual intervention required.", sku);
                    // In a full Saga pattern, we would throw an exception here and publish an OrderFailedEvent to rollback the order.
                }

                inventory.setQuantity(newQuantity);
                inventoryRepository.save(inventory);
                log.info("Deducted {} units of {}. Remaining stock: {}", quantityToDeduct, sku, newQuantity);
            }
        } catch (Exception e) {
            log.error("Failed to process inventory deduction event", e);
        }
    }

}
