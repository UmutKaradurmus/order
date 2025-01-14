package com.micro.order.controller;

import com.micro.order.dto.CancelOrderRequest;
import com.micro.order.dto.CreateOrderRequest;
import com.micro.order.entity.Order;
import com.micro.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Yeni bir sipariş oluştur")
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order createdOrder = orderService.createOrder(request);
        return ResponseEntity.ok(createdOrder);
    }

    @Operation(summary = "Siparişi iptal et")
    @PostMapping("/cancel")
    public ResponseEntity<Order> cancelOrder(@RequestBody CancelOrderRequest request) {
        Order canceledOrder = orderService.cancelOrder(request);
        return ResponseEntity.ok(canceledOrder);
    }

    @Operation(summary = "Tüm siparişleri getir")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "ID'ye göre sipariş getir")
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Belirli bir kullanıcıya ait tüm siparişleri getir")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }
}
