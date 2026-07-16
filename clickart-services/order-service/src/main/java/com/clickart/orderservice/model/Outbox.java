package com.clickart.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Outbox {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The Kafka topic this event should be sent to
    private String aggregateType; 

    // The UUID of the order
    private String aggregateId; 

    // The actual JSON payload of the event
    @Column(columnDefinition = "TEXT")
    private String payload; 

    // Indicates if the background poller has sent this to Kafka yet
    private boolean processed; 

    private LocalDateTime createdAt;
}
