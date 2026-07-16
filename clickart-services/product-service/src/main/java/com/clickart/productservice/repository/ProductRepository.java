package com.clickart.productservice.repository;

import com.clickart.productservice.model.Product;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

// By extending MongoRepository, Spring automatically generates all basic CRUD SQL queries behind the scenes.
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findBySkuCodeIn(List<String> arrayList);
}