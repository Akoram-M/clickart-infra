package com.clickart.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/product")
    public ResponseEntity<String> productGetFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Product Service is currently offline. Please try again later.");
    }

    @PostMapping("/product")
    public ResponseEntity<String> productPostFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Cannot create products at this time. Product Service is unreachable.");
    }

    @GetMapping("/inventory")
    public ResponseEntity<String> inventoryGetFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Inventory Service is currently offline. Please try again later.");
    }

    @PostMapping("/inventory")
    public ResponseEntity<String> inventoryPostFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Cannot create/update Inventory at this time. Inventory Service is unreachable.");
    }

    @GetMapping("/order")
    public ResponseEntity<String> orderGetFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Order Service is currently offline. Please try again later.");
    }

    @PostMapping("/order")
    public ResponseEntity<String> orderPostFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Cannot place order at this time. Order Service is unreachable.");
    }
}