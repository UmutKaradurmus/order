package com.micro.order.dto;

import lombok.Data;


@Data
public class CreateOrderRequest {
    private Long userId; // Kullanıcının ID'si
    private Long cartId; // Sepetin ID'si
}

