package com.clickart.productservice.service;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.clickart.productservice.dto.OrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class TrendingProductService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper; // Inject Jackson
    private static final String TRENDING_KEY = "trending_products";

    @KafkaListener(topics = "order-placed-topic", groupId = "product-group")
    public void updateTrendingScore(String eventString) {
        log.info("TrendingProductService :: updateTrendingScore <--");
        try {
            // manually serialize
            OrderPlacedEvent event = objectMapper.readValue(eventString, OrderPlacedEvent.class);
            log.info("Ingesting trending event for order: {}", event.getOrderNumber());

            for (Map.Entry<String,Integer> entry : event.getSkuQuantities().entrySet()) {
                String skuCode = entry.getKey();
                stringRedisTemplate.opsForZSet().incrementScore(TRENDING_KEY, skuCode, 1.0);
                log.info("Extracted SKUs to increment: {}", skuCode); 
            }
        } catch (Exception e) {
            log.error("Failede to deserialize kafka trending events");
        }
        log.info("TrendingProductService :: updateTrendingScore -->");
    }
}
