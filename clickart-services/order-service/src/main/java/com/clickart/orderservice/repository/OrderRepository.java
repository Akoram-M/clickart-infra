package com.clickart.orderservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clickart.orderservice.model.Order;

// JpaRepository handles all the standard SQL queries for the Order entity.
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumbeString);
}