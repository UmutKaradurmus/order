package com.micro.order.repository;

import com.micro.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Kullanıcı bazlı siparişleri getir
    List<Order> findByUserId(Long userId);
}