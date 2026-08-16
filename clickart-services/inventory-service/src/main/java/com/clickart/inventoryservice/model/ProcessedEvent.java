package com.clickart.inventoryservice.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    @Id
    private String eventId; // The unique UUID from Kafka header
    private LocalDateTime processedAt;
}