package com.micro.order.dto;

import lombok.Data;

@Data
public class CancelOrderRequest {
    private Long orderId;
    private String reason;
}
