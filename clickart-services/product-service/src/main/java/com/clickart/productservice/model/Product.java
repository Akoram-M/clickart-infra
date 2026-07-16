package com.clickart.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(value = "product") // Tells Mongo this is a collection
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data // Lombok: auto-generates getters, setters, toString
public class Product {
    @Id
    private String id; // Mongo IDs are Strings, not Longs
    private String skuCode;
    private String name;
    private String description;
    private BigDecimal price; // Always use BigDecimal for money, never double
    @CreatedDate
    private LocalDateTime createdDateTime;
}