package com.clickart.inventoryservice;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.clickart.inventoryservice.model.Inventory;
import com.clickart.inventoryservice.repository.InventoryRepository;
import com.clickart.inventoryservice.repository.ProcessedEventRepository;

class InventoryIdempotencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private static final String TOPIC = "order-placed-topic";
    private static final String SKU_CODE = "CRUCIAL_RAM_32GB_DDR5";

    @BeforeEach
    void setup() {
        processedEventRepository.deleteAll();
        inventoryRepository.deleteAll();

        Inventory initialStock = new Inventory();
        initialStock.setSkuCode(SKU_CODE);
        initialStock.setQuantity(10);
        inventoryRepository.save(initialStock);
    }

    @Test
    @DisplayName("Should deduct stock once and drop duplicate event with identical eventId")
    void shouldDeductStockOnceAndIgnoreDuplicateEvents() {
        String eventId = "TEST-EVENT-" + System.currentTimeMillis();
        String payload = "{\"orderNumber\":\"ORD-TC-001\",\"skuQuantities\":{\"" + SKU_CODE + "\":2}}";

        ProducerRecord<String, String> record1 = new ProducerRecord<>(TOPIC, payload);
        record1.headers().add(new RecordHeader("eventId", eventId.getBytes(StandardCharsets.UTF_8)));

        // 1. Send first event
        kafkaTemplate.send(record1);

        // 2. Await async consumption and verify DB state (Quantity must drop from 10 to 8)
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Inventory updated = inventoryRepository.findBySkuCode(SKU_CODE).orElseThrow();
                    assertEquals(8, updated.getQuantity(), "Stock should be deducted by 2");
                    assertTrue(processedEventRepository.existsById(eventId), "Event ID must be recorded");
                });

        // 3. Send duplicate event with identical eventId
        ProducerRecord<String, String> duplicateRecord = new ProducerRecord<>(TOPIC, payload);
        duplicateRecord.headers().add(new RecordHeader("eventId", eventId.getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(duplicateRecord);

        // 4. Wait to ensure consumer picks it up, but verify stock remains strictly 8 (Deduplication worked)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignored) {}

        Inventory finalStock = inventoryRepository.findBySkuCode(SKU_CODE).orElseThrow();
        assertEquals(8, finalStock.getQuantity(), "Duplicate event must NOT deduct stock a second time");
    }
}