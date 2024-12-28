package com.micro.order.service;

import com.micro.order.dto.CancelOrderRequest;
import com.micro.order.dto.CreateOrderRequest;
import com.micro.order.entity.Order;
import com.micro.order.repository.OrderRepository;
import com.micro.order.util.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // Basitlik adına RestTemplate ile logger'a istek atıyoruz
    // (WebClient de kullanılabilir)
    private final RestTemplate restTemplate = new RestTemplate();

    // Logger mikroservisi URL'ini application.yml'dan alabilirsiniz
    @Value("${logger.service-url}")
    private String loggerServiceUrl;
    // Not: Docker Compose'da "logger-service" adındaysa: "http://logger-service:8081"

    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setCartId(request.getCartId());
        order.setCreatedAt(LocalDateTime.now());

        // Mock ödeme senaryosu (örnek)
        if (new Random().nextBoolean()) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        Order saved = orderRepository.save(order);

        // Ödeme sonucu SUCCESS ise "order placed" logu, FAILED ise "payment failed" logu
        sendLogToLogger("ORDER_SERVICE", "Order created with paymentStatus="
                + saved.getPaymentStatus() + " orderId=" + saved.getId());

        return saved;
    }

    public Order cancelOrder(CancelOrderRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderId()));

        order.setCanceled(true);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        sendLogToLogger("ORDER_SERVICE", "Order canceled. ID=" + order.getId()
                + " reason=" + request.getReason());

        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Basit bir log kaydı için Logger mikroservisine HTTP POST isteği atıyoruz.
     * Logger servisinde "/api/logs" gibi bir endpoint olduğunu varsayalım.
     */
    private void sendLogToLogger(String source, String message) {
        // JSON body yollamak istersek küçük bir Map oluşturabiliriz
        var logBody = new LogRequest(source, message);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    loggerServiceUrl + "/api/logs",
                    logBody,
                    String.class
            );
            // response.getStatusCode() ile vs. loglamaya devam edilebilir
        } catch (Exception e) {
            e.printStackTrace();
            // Logger down ise vb. hata alabilirsiniz, asenkron bir kuyruk ile de yollanabilir.
        }
    }

    // Basit DTO
    record LogRequest(String source, String message) {}
}
