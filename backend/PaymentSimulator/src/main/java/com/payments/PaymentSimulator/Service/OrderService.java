package com.payments.PaymentSimulator.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.payments.PaymentSimulator.Model.Order;
import com.payments.PaymentSimulator.Model.OrderItem;

@Service
public class OrderService {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order createOrder(String customerId, List<OrderItem> items) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerId(customerId);
        order.setItems(new ArrayList<>(items));
        order.setStatus(Order.OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        BigDecimal total = items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        
        orders.put(order.getOrderId(), order);
        return order;
    }

    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Order updateOrderStatus(String orderId, Order.OrderStatus status, String failureReason) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            if (failureReason != null) {
                order.setFailureReason(failureReason);
            }
        }
        return order;
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
}

