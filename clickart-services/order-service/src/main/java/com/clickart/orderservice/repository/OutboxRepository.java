package com.clickart.orderservice.repository;

import com.clickart.orderservice.model.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    // Custom method for the background thread to find unprocessed events
    List<Outbox> findByProcessedFalse();
}