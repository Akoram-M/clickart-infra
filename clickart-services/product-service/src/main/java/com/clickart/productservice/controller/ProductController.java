package com.clickart.productservice.controller;

import com.clickart.productservice.model.Product;
import com.clickart.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor // Lombok creates a constructor for the injected repository automatically
public class ProductController {

    private final ProductRepository productRepository;

    @PostMapping("/addProducts")
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@RequestBody List<Product> products) {
        productRepository.saveAll(products);
    }

    @GetMapping("/getAllProducts")
    @ResponseStatus(HttpStatus.OK)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}