package com.clickart.orderservice.service;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.clickart.orderservice.model.Outbox;
import com.clickart.orderservice.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Runs every 5 seconds
    @Scheduled(fixedDelay = 5000) 
    public void pollOutboxEvents() {
        List<Outbox> unprocessedEvents = outboxRepository.findByProcessedFalse();

        if (unprocessedEvents.isEmpty()) {
            return;
        }

        log.info("Found {} unprocessed outbox events", unprocessedEvents.size());

        for (Outbox event : unprocessedEvents) {
            try {
                // 1. Send to Kafka
                // Using "order-placed-topic" as the unified topic for all downstream consumers
                kafkaTemplate.send("order-placed-topic", event.getPayload());

                // 2. Mark as processed ONLY if Kafka send succeeds
                event.setProcessed(true);
                outboxRepository.save(event);
                
                log.info("Successfully dispatched outbox event for Order: {}", event.getAggregateId());
                
            } catch (Exception e) {
                // If Kafka is down, the loop catches the error, leaves processed=false, and tries again in 5 seconds
                log.error("Failed to send outbox event to Kafka. Will retry. Error: {}", e.getMessage());
            }
        }
    }
}