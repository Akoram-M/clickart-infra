package com.clickart.orderservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clickart.orderservice.model.Order;
import com.clickart.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController<inventoryClient> {

    private final OrderService orderService;

    @PostMapping("/placeOrders")
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    // Call with: GET /api/orders
    // Or call with: GET /api/orders?status=CONFIRMED
    @GetMapping
    public List<Order> getOrders(@RequestParam(required = false) String status) {
        return orderService.listPlacedOrders(status);
    }
}