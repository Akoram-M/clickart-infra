package com.clickart.orderservice.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.clickart.orderservice.model.Order;
import com.clickart.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderSagaListner {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "inventory-success-topic", groupId = "order-group")
    public void handleInventorySuccess(String orderNumber) {
        log.info("Saga Step: Inventory Approved. Confirming Order {}", orderNumber);
        updateOrderStatus(orderNumber, "CONFIRMED");
    }

    @KafkaListener(topics = "inventory-failed-topic", groupId = "order-group")
    public void handleInventoryFailure(String orderNumber) {
        log.info("Saga Step: Inventory Rejected. Cancelling Order {}", orderNumber);
        updateOrderStatus(orderNumber, "CANCELLED");
    }


    private void updateOrderStatus(String orderNumber, String status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order Not found : " + orderNumber));
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Order {} status updated to {}", orderNumber, status);
    }
}
