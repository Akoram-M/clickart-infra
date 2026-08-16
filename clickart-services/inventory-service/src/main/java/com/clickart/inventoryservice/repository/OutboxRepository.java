package com.clickart.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clickart.inventoryservice.model.OutboxEvent;

/**
 * OutboxRepository
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

}
