package com.clickart.productservice.controller;

import com.clickart.productservice.model.Product;
import com.clickart.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor // Lombok creates a constructor for the injected repository automatically
@Slf4j
public class ProductController {

    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;

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

    @GetMapping("/trending")
    @ResponseStatus(HttpStatus.OK)
    public List<Product> getTrendingProducts() {
        // 1. Fetch Top 10 SKUs from Redis ZSET (index 0 to 9)
        Set<String> topSkus = stringRedisTemplate.opsForZSet().reverseRange("trending_products", 0, 9);

        if (topSkus == null || topSkus.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Fetch full metadata from MongoDB
        return productRepository.findBySkuCodeIn(new ArrayList<>(topSkus));
    }
}