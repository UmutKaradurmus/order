package com.micro.order.dto;

public class ProductSchema {
    private long id;
    private int amount;

    public ProductSchema(Long id, int amount) {
        this.id = id;
        this.amount = amount;
    }
    // Default constructor
    public ProductSchema() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
