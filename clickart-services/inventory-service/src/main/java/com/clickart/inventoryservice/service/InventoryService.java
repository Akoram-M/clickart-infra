package com.clickart.inventoryservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

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
            String orderNumber = event.getOrderNumber();

            log.info("Deducting inventory for Order: {}", orderNumber);

            Boolean allInStock = true;
            for (Map.Entry<String, Integer> entry : event.getSkuQuantities().entrySet()) {
                String sku = entry.getKey();
                Integer quantityToDeduct = entry.getValue();

                Inventory inventory = inventoryRepository.findBySkuCode(sku)
                        .orElseThrow(() -> new RuntimeException("SKU not found in inventory: " + sku));

                if (inventory.getQuantity() < quantityToDeduct) {
                    allInStock = false;
                    log.warn("Insufficient stock for SKU: {}. Required: {}, Available: {}",
                            entry.getKey(), entry.getValue(), inventory.getQuantity());
                    break; // Fail the entire order
                }
            }
            if (allInStock) {
                for (Map.Entry<String, Integer> entry : event.getSkuQuantities().entrySet()) {
                    Inventory inventory = inventoryRepository.findBySkuCode(entry.getKey()).get();
                    inventory.setQuantity(inventory.getQuantity() - entry.getValue());
                    inventoryRepository.save(inventory);
                }
                log.info("Inventory deducted. Approving order {}", orderNumber);
                kafkaTemplate.send("inventory-success-topic", orderNumber);
            } else {
                log.error("Order {} failed inventory check. Rejecting.", orderNumber);
                kafkaTemplate.send("inventory-failed-topic", orderNumber);
            }
        } catch (Exception e) {
            log.error("Failed to process inventory deduction event", e);
        }
    }

}
