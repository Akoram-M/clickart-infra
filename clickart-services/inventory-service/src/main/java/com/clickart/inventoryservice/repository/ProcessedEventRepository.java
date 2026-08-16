package com.clickart.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clickart.inventoryservice.model.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}