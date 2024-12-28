package com.micro.order.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private Long userId;
    private Long cartId;
}