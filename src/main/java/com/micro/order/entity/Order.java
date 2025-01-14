package com.micro.order.entity;

import com.micro.order.util.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Siparişi veren kullanıcının ID'si
    private Long cartId; // İlgili sepetin ID'si

    private LocalDateTime createdAt; // Siparişin oluşturulma tarihi
    private LocalDateTime updatedAt; // Siparişin güncellenme tarihi (ör. iptal durumunda)

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // Ödeme durumu (SUCCESS, FAILED vb.)

    private boolean canceled; // Siparişin iptal edilip edilmediği

    // Ürün listesi: Bir siparişte birden fazla ürün olabilir
    @ElementCollection
    @CollectionTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderProduct> products; // Siparişe ait ürünlerin listesi
}
