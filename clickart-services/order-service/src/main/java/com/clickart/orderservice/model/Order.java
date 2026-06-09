package com.clickart.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in Postgres, so we use "orders"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments 1, 2, 3 in Postgres
    private Long id;
    
    private String orderNumber;
    
    private String status; // PENDING, CONFIRMED, FAILED

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderLineItem> orderLineItems;
}