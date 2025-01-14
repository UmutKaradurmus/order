package com.micro.order.dto;

import lombok.Data;

@Data
public class CancelOrderRequest {
    private Long orderId;

    public CancelOrderRequest(Long orderId) {
        this.orderId = orderId;
    }
}
