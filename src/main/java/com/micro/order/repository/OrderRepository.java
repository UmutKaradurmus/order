package com.micro.order.repository;

import com.micro.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Gerekirse özel sorgular ekleyebilirsiniz
}