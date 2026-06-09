package com.clickart.orderservice.controller;

import com.clickart.orderservice.model.Order;
import com.clickart.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;

    @PostMapping("/placeOrders")
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody List<Order> orders) {
        for (Order data : orders) {
            // Assign a random order number and set the initial state
            data.setOrderNumber(UUID.randomUUID().toString());
            data.setStatus("PENDING");
        }
        orderRepository.saveAll(orders);
        return "Order Placed Successfully";
    }
}