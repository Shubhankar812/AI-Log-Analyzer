package com.payments.PaymentSimulator.Service;


import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class PaymentService {
    private final Random random = new Random();

    public boolean processPayment(String orderId, String paymentMethod, boolean simulateFailure) {
        if (simulateFailure) {
            return false;
        }
        
        // Simulate payment gateway timeout (5% chance)
        if (random.nextInt(20) == 0) {
            throw new RuntimeException("Payment gateway timeout");
        }
        
        // Simulate insufficient funds (8% chance)
        if (random.nextInt(12) == 0) {
            return false;
        }
        
        // Simulate processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return true;
    }
}
