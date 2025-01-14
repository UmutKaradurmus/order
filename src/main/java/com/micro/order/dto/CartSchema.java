package com.micro.order.dto;

import java.util.List;

public class CartSchema {
    private Long id;
    private Long userId;
    private List<ProductSchema> products;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<ProductSchema> getProducts() {
        return products;
    }

    public void setProducts(List<ProductSchema> products) {
        this.products = products;
    }
}
