package com.micro.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.micro.order.client.RabbitMQClient;
import com.micro.order.dto.CancelOrderRequest;
import com.micro.order.dto.CartSchema;
import com.micro.order.dto.CreateOrderRequest;
import com.micro.order.entity.Order;
import com.micro.order.entity.OrderProduct;
import com.micro.order.repository.OrderRepository;
import com.micro.order.util.PaymentStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitMQClient rabbitMQClient;
    private final ObjectMapper objectMapper;

    private static final String LOG_QUEUE = "log_service_queue";

    // Yeni bir sipariş oluştur
    public Order createOrder(CreateOrderRequest request) {
        logToService("INFO", "Create order process started for userId: " + request.getUserId() + ", cartId: " + request.getCartId());

        // RabbitMQ üzerinden Cart Service'ten sepet bilgilerini al
        CartSchema cart = getCartById(request.getCartId());
        if (cart == null || cart.getProducts().isEmpty()) {
            logToService("ERROR", "Cart is empty or not found for cartId: " + request.getCartId());
            throw new RuntimeException("Cart is empty or not found for cartId: " + request.getCartId());
        }

        // cartId doğrulaması
        if (!cart.getUserId().equals(request.getUserId())) {
            logToService("ERROR", "Cart does not belong to userId: " + request.getUserId());
            throw new RuntimeException("Cart does not belong to userId: " + request.getUserId());
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setCartId(request.getCartId());
        order.setCreatedAt(LocalDateTime.now());
        order.setProducts(cart.getProducts().stream()
                .map(product -> {
                    OrderProduct orderProduct = new OrderProduct();
                    orderProduct.setProductId(product.getId());
                    orderProduct.setQuantity(product.getAmount());
                    return orderProduct;
                }).toList());

        // Ödeme işlemi
        if (getRandomBoolean()) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            clearCart(request.getCartId()); // Sepeti temizle
            updateProductStock(order.getProducts(), "decrease_stock"); // Stokları azalt
            logToService("INFO", "Order created successfully for userId: " + request.getUserId() + ", cartId: " + request.getCartId());
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            logToService("WARN", "Order creation failed for userId: " + request.getUserId() + ", cartId: " + request.getCartId());
        }

        return orderRepository.save(order);
    }


    // Siparişi iptal et
    public Order cancelOrder(CancelOrderRequest request) {
        logToService("INFO", "Cancel order process started for orderId: " + request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> {
                    logToService("ERROR", "Order not found for orderId: " + request.getOrderId());
                    return new RuntimeException("Order not found: " + request.getOrderId());
                });


        order.setCanceled(true);
        order.setPaymentStatus(PaymentStatus.CANCELED); // PaymentStatus güncelleniyor
        order.setUpdatedAt(LocalDateTime.now());

        updateProductStock(order.getProducts(), "increase_stock"); // Ürün stokları artırılıyor
        logToService("INFO", "Order canceled successfully for orderId: " + request.getOrderId());

        return orderRepository.save(order);
    }


    // Tüm siparişleri getir
    @Transactional
    public List<Order> getAllOrders() {
        logToService("INFO", "Fetching all orders");
        return orderRepository.findAll();
    }

    // ID'ye göre sipariş getir
    public Order getOrderById(Long id) {
        logToService("INFO", "Fetching order by ID: " + id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    logToService("ERROR", "Order not found with ID: " + id);
                    return new RuntimeException("Order not found with id: " + id);
                });
    }

    // Kullanıcı bazlı siparişleri getir
    public List<Order> getOrdersByUserId(Long userId) {
        logToService("INFO", "Fetching orders for user ID: " + userId);
        return orderRepository.findByUserId(userId);
    }

    // RabbitMQ üzerinden belirli bir sepeti getir
    private CartSchema getCartById(Long cartId) {
        String message = "{ \"action\": \"get_cart_by_id\", \"cart_id\": " + cartId + " }";
        String response = rabbitMQClient.sendAndReceive("get_cart_request", message);

        if (response == null || response.isEmpty()) {
            logToService("ERROR", "Cart is empty or not found for cartId: " + cartId);
            throw new RuntimeException("Cart is empty or not found for cartId: " + cartId);
        }

        try {
            return objectMapper.readValue(response, CartSchema.class);
        } catch (Exception e) {
            logToService("ERROR", "Failed to parse cart data for cartId: " + cartId);
            throw new RuntimeException("Failed to parse cart data for cartId: " + cartId, e);
        }
    }

    // RabbitMQ üzerinden ürün stoklarını güncelle
    private void updateProductStock(List<OrderProduct> products, String action) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("{ \"action\": \"").append(action).append("\", \"products\": [");
        for (OrderProduct product : products) {
            messageBuilder.append("{ \"id\": ").append(product.getProductId())
                    .append(", \"amount\": ").append(product.getQuantity()).append(" },");
        }
        String message = messageBuilder.substring(0, messageBuilder.length() - 1) + "] }";
        rabbitMQClient.sendAndReceive("product_service_queue", message);
        logToService("INFO", "Product stock updated with action: " + action + ", products: " + message);
    }

    // RabbitMQ üzerinden sepeti temizle
    private void clearCart(Long cartId) {
        String message = "{ \"action\": \"delete_cart\", \"cart_id\": " + cartId + " }";
        rabbitMQClient.sendAndReceive("cart_service_queue", message);
        logToService("INFO", "Cart cleared successfully for cartId: " + cartId);
    }

    // RabbitMQ üzerinden log gönder
    private void logToService(String level, String message) {
        try {
            String logPayload = objectMapper.writeValueAsString(new LogRequest("order-service", level, message));
            rabbitMQClient.sendAndReceive(LOG_QUEUE, logPayload);
        } catch (Exception e) {
            System.err.println("Failed to send log to RabbitMQ: " + message);
        }
    }

    // Rastgele boolean üreten yardımcı metot (ör. ödeme durumu için)
    protected boolean getRandomBoolean() {
        return new Random().nextBoolean();
    }

    // Log isteği için DTO
    private static class LogRequest {
        private String service;
        private Content content;

        public LogRequest(String service, String level, String message) {
            this.service = service;
            this.content = new Content(level, message);
        }

        private static class Content {
            private String level;
            private String message;

            public Content(String level, String message) {
                this.level = level;
                this.message = message;
            }
        }
    }
}
