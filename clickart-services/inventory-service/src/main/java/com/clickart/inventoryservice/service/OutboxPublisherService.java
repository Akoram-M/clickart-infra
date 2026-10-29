// package com.clickart.inventoryservice.service;

// import java.util.List;

// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.clickart.inventoryservice.model.OutboxEvent;
// import com.clickart.inventoryservice.repository.OutboxRepository;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class OutboxPublisherService {

//     private final OutboxRepository outboxRepository;
//     private final KafkaTemplate<String, String> kafkaTemplate;

//     @Scheduled(fixedDelay = 2000)
//     @Transactional
//     public void publishOutboxEvents() {
//         List<OutboxEvent> pendingEvents = outboxRepository.findUnprocessedEvents();

//         if (pendingEvents.isEmpty()) {
//             return;
//         }

//         log.info("Found {} unprocessed outbox events. Relaying to Kafka...", pendingEvents.size());

//         for (OutboxEvent event : pendingEvents) {
//             try {
//                 // Route dynamically based on eventType or aggregate domain
//                 String targetTopic = determineTopic(event.getEventType());

//                 kafkaTemplate.send(targetTopic, event.getAggregateId(), event.getPayload())
//                         .whenComplete((result, ex) -> {
//                             if (ex == null) {
//                                 log.debug("Outbox event ID: {} relayed successfully to {}", event.getId(), targetTopic);
//                             } else {
//                                 log.error("Failed to relay outbox event ID: {}", event.getId(), ex);
//                             }
//                         });

//                 // Mark processed in the same transactional cycle
//                 event.setProcessed(true);
//                 outboxRepository.save(event);

//             } catch (Exception e) {
//                 log.error("Error dispatching outbox event ID: {}", event.getId(), e);
//                 // Do not mark as processed; retry on next poll cycle
//             }
//         }
//     }

//     private String determineTopic(String eventType) {
//         return switch (eventType) {
//             case "InventoryProcessed" -> "inventory-success-topic";
//             case "InventoryFailed" -> "inventory-failed-topic";
//             default -> "inventory-events-dlq";
//         };
//     }
// }