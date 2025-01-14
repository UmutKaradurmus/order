package com.micro.order.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class OrderProduct {
    private Long productId; // Ürün ID'si
    private int quantity;   // Ürün miktarı

    public OrderProduct(long l, int i) {
    }

    public OrderProduct() {

    }
}
