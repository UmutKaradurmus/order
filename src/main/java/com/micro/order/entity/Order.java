package com.micro.order.entity;

import com.micro.order.util.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long cartId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Ödeme senaryosu: BAŞARILI, BAŞARISIZ vb.
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private boolean canceled;
}
