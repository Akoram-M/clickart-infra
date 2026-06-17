package com.clickart.orderservice.controller;

import com.clickart.orderservice.client.InventoryClient;
import com.clickart.orderservice.model.Order;
import com.clickart.orderservice.model.OrderLineItem;
import com.clickart.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController<inventoryClient> {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @PostMapping("/placeOrders")
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody Order order) {
        log.info("OrderController : placeOrder : start");
        // Loop through all items in the payload and query the Inventory container over HTTP
        for (OrderLineItem item : order.getOrderLineItems()) {
            boolean inStock = inventoryClient.isInStock(item.getSkuCode());
            if (!inStock) {
                // If the Inventory Service returns false, abort the transaction immediately
                throw new IllegalArgumentException("Product is not in stock: " + item.getSkuCode());
            }
        }

        order.setOrderNumber(UUID.randomUUID().toString());
        order.setStatus("CONFIRMED"); // Stock exists, transaction approved
        
        orderRepository.save(order);
        log.info("OrderController : placeOrder : end");
        return "Order Placed Successfully";
    }
}