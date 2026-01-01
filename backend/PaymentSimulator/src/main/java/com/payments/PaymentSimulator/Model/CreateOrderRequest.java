package com.payments.PaymentSimulator.Model;

import java.util.List;

public class CreateOrderRequest {
    private String customerId;
    private List<OrderItem> items;

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}

