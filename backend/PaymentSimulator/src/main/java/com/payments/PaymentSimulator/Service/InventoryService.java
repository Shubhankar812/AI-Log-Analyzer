package com.payments.PaymentSimulator.Service;


import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class InventoryService {
    private final Random random = new Random();

    public boolean reserveInventory(String orderId, boolean simulateFailure) {
        if (simulateFailure) {
            return false;
        }
        
        // Simulate 10% random failure rate
        if (random.nextInt(10) == 0) {
            return false;
        }
        
        // Simulate processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return true;
    }
}