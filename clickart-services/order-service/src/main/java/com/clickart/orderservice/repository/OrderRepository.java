package com.clickart.orderservice.repository;

import com.clickart.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository handles all the standard SQL queries for the Order entity.
public interface OrderRepository extends JpaRepository<Order, Long> {
}