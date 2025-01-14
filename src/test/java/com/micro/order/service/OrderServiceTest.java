package com.micro.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.micro.order.client.RabbitMQClient;
import com.micro.order.dto.CancelOrderRequest;
import com.micro.order.dto.CartSchema;
import com.micro.order.dto.CreateOrderRequest;
import com.micro.order.dto.ProductSchema;
import com.micro.order.entity.Order;
import com.micro.order.entity.OrderProduct;
import com.micro.order.repository.OrderRepository;
import com.micro.order.util.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitMQClient rabbitMQClient;

    @InjectMocks
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Order mockOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, rabbitMQClient, objectMapper);

        // Mock Order Nesnesi
        mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setUserId(101L);
        mockOrder.setCartId(1L);
        mockOrder.setPaymentStatus(PaymentStatus.SUCCESS);
        mockOrder.setCanceled(false);
        mockOrder.setCreatedAt(LocalDateTime.now());
        mockOrder.setProducts(List.of(
                new OrderProduct(201L, 2),
                new OrderProduct(202L, 1)
        ));
    }

    @Test
    void testCreateOrderSuccess() throws Exception {
        // Mock CartSchema
        CartSchema cart = new CartSchema();
        cart.setId(1L);
        cart.setUserId(101L);
        cart.setProducts(List.of(new ProductSchema(201L, 2), new ProductSchema(202L, 1)));

        // Mock RabbitMQ yanıtları
        when(rabbitMQClient.sendAndReceive(eq("get_cart_request"), anyString()))
                .thenReturn(objectMapper.writeValueAsString(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock the getRandomBoolean method to always return true
        OrderService spyOrderService = spy(orderService);
        doReturn(true).when(spyOrderService).getRandomBoolean();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(101L);
        request.setCartId(1L);

        Order order = spyOrderService.createOrder(request);

        assertNotNull(order);
        assertEquals(101L, order.getUserId());
        assertEquals(1L, order.getCartId());
        assertEquals(PaymentStatus.SUCCESS, order.getPaymentStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(rabbitMQClient, times(1)).sendAndReceive(eq("cart_service_queue"), contains("delete_cart"));
        verify(rabbitMQClient, times(1)).sendAndReceive(eq("product_service_queue"), contains("decrease_stock"));
    }

    @Test
    void testCreateOrderWithEmptyCart() {
        // Mock RabbitMQ yanıtı
        when(rabbitMQClient.sendAndReceive(eq("get_cart_request"), anyString())).thenReturn(null);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(101L);
        request.setCartId(1L);

        Exception exception = assertThrows(RuntimeException.class, () -> orderService.createOrder(request));

        assertEquals("Cart is empty or not found for cartId: 1", exception.getMessage());
        verify(rabbitMQClient, times(1)).sendAndReceive(eq("get_cart_request"), anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCancelOrderSuccess() {
        // Mock orderRepository findById davranışı
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelOrderRequest request = new CancelOrderRequest(1L);

        Order canceledOrder = orderService.cancelOrder(request);

        assertNotNull(canceledOrder);
        assertTrue(canceledOrder.isCanceled());
        assertEquals(PaymentStatus.CANCELED, canceledOrder.getPaymentStatus()); // Status kontrolü
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(rabbitMQClient, times(1)).sendAndReceive(eq("product_service_queue"), contains("increase_stock"));
    }


    @Test
    void testCancelOrderNotFound() {
        // Mock orderRepository findById davranışı
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        CancelOrderRequest request = new CancelOrderRequest(1L);

        Exception exception = assertThrows(RuntimeException.class, () -> orderService.cancelOrder(request));

        assertEquals("Order not found: 1", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(rabbitMQClient, never()).sendAndReceive(eq("product_service_queue"), anyString());
    }

    @Test
    void testGetAllOrders() {
        // Mock findAll davranışı
        when(orderRepository.findAll()).thenReturn(List.of(mockOrder, new Order()));

        List<Order> orders = orderService.getAllOrders();

        assertNotNull(orders);
        assertEquals(2, orders.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrderById() {
        // Mock findById davranışı
        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));

        Order order = orderService.getOrderById(1L);

        assertNotNull(order);
        assertEquals(1L, order.getId());
        assertEquals(101L, order.getUserId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrdersByUserId() {
        // Mock findByUserId davranışı
        when(orderRepository.findByUserId(101L)).thenReturn(List.of(mockOrder));

        List<Order> orders = orderService.getOrdersByUserId(101L);

        assertNotNull(orders);
        assertEquals(1, orders.size());
        assertEquals(101L, orders.get(0).getUserId());
        verify(orderRepository, times(1)).findByUserId(101L);
    }
}
