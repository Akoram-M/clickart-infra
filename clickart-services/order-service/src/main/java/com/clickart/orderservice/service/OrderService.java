package com.clickart.orderservice.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clickart.orderservice.dto.OrderPlacedEvent;
import com.clickart.orderservice.model.Order;
import com.clickart.orderservice.model.OrderLineItem;
import com.clickart.orderservice.model.Outbox;
import com.clickart.orderservice.repository.OrderRepository;
import com.clickart.orderservice.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public String placeOrder(Order order) {
        log.info("OrderService : placeOrder : <-->");
        // 1. Prepare the Order
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setStatus("PENDING");

        // 2. Save the Order to Postgres
        orderRepository.save(order);
        log.info("Order Saved: {}", order.getOrderNumber());

        // 3. Extract SKUs AND Quantities for the event
        Map<String, Integer> skuQuantities = order.getOrderLineItems().stream()
                .collect(Collectors.toMap(
                        OrderLineItem::getSkuCode,
                        OrderLineItem::getQuantity));

        // 4. Create and Save the Outbox Event (NO KAFKA CALLS HERE)
        try {
            OrderPlacedEvent eventPayload = new OrderPlacedEvent(order.getOrderNumber(), skuQuantities);
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType("order-placed")
                    .aggregateId(order.getOrderNumber())
                    .payload(jsonPayload)
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox Event Saved for PENDING Order: {}", order.getOrderNumber());

        } catch (Exception e) {
            // If serialization fails, the entire transaction rolls back. Order is NOT
            // saved.
            throw new RuntimeException("Failed to create outbox event", e);
        }

        log.info("OrderService : placeOrder : -->");
        return "Order Placed and is currently PENDING";
    }

}
