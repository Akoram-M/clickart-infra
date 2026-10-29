package com.clickart.inventoryservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.clickart.inventoryservice.model.OutboxEvent;

/**
 * OutboxRepository
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // @Query(value = "SELECT * FROM outbox_events WHERE processed = false ORDER BY id ASC LIMIT 50", nativeQuery = true)
    // List<OutboxEvent> findUnprocessedEvents();
}
