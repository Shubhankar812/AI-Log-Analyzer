package com.payments.PaymentSimulator.Controller;


import java.util.List;

import com.payments.PaymentSimulator.Service.InventoryService;
import com.payments.PaymentSimulator.Service.OrderService;
import com.payments.PaymentSimulator.Service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payments.PaymentSimulator.Model.ApiResponse;
import com.payments.PaymentSimulator.Model.CreateOrderRequest;
import com.payments.PaymentSimulator.Model.Order;
import com.payments.PaymentSimulator.Model.PaymentRequests;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, InventoryService inventoryService, 
                          PaymentService paymentService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order order = orderService.createOrder(request.getCustomerId(), request.getItems());
            return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to create order: " + e.getMessage()));
        }
    }

    @PostMapping("/orders/{orderId}/reserve-inventory")
    public ResponseEntity<ApiResponse<Order>> reserveInventory(
            @PathVariable String orderId,
            @RequestParam(required = false, defaultValue = "false") boolean simulateFailure) {
        
        return orderService.getOrder(orderId)
            .map(order -> {
                if (order.getStatus() != Order.OrderStatus.CREATED) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.<Order>error("Order must be in CREATED status"));
                }
                
                boolean success = inventoryService.reserveInventory(orderId, simulateFailure);
                
                if (success) {
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.INVENTORY_RESERVED, null);
                    return ResponseEntity.ok(
                        ApiResponse.success("Inventory reserved successfully", order));
                } else {
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.FAILED, 
                        "Insufficient inventory");
                    return ResponseEntity.ok(
                        ApiResponse.<Order>error("Failed to reserve inventory"));
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders/{orderId}/process-payment")
    public ResponseEntity<ApiResponse<Order>> processPayment(
            @PathVariable String orderId,
            @RequestBody PaymentRequests paymentRequest) {
        
        return orderService.getOrder(orderId)
            .map(order -> {
                if (order.getStatus() != Order.OrderStatus.INVENTORY_RESERVED) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.<Order>error("Inventory must be reserved first"));
                }
                
                try {
                    boolean success = paymentService.processPayment(
                        orderId, 
                        paymentRequest.getPaymentMethod(), 
                        paymentRequest.isSimulateFailure()
                    );
                    
                    if (success) {
                        orderService.updateOrderStatus(orderId, 
                            Order.OrderStatus.PAYMENT_PROCESSED, null);
                        return ResponseEntity.ok(
                            ApiResponse.success("Payment processed successfully", order));
                    } else {
                        orderService.updateOrderStatus(orderId, Order.OrderStatus.FAILED, 
                            "Payment declined");
                        return ResponseEntity.ok(
                            ApiResponse.<Order>error("Payment failed"));
                    }
                } catch (Exception e) {
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.FAILED, 
                        e.getMessage());
                    return ResponseEntity.ok(
                        ApiResponse.<Order>error("Payment processing error: " + e.getMessage()));
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Order>> confirmOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
            .map(order -> {
                if (order.getStatus() != Order.OrderStatus.PAYMENT_PROCESSED) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.<Order>error("Payment must be processed first"));
                }
                
                orderService.updateOrderStatus(orderId, Order.OrderStatus.CONFIRMED, null);
                return ResponseEntity.ok(
                    ApiResponse.success("Order confirmed successfully", order));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
            .map(order -> ResponseEntity.ok(ApiResponse.success("Order found", order)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orders));
    }
}