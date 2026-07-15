package com.clickart.productservice.controller;

import com.clickart.productservice.model.Product;
import com.clickart.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor // Lombok creates a constructor for the injected repository automatically
@Slf4j
public class ProductController {

    private final ProductRepository productRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(value = "products", allEntries = true)
    public void addProduct(@RequestBody List<Product> products) {
        productRepository.saveAll(products);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        log.info("productController :: getAllProducts() ");
        return productRepository.findAll();
    }
}