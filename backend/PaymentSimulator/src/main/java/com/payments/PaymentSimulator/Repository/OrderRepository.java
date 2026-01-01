package com.payments.PaymentSimulator.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payments.PaymentSimulator.Model.Order;
//import org.springframework.stereotype.Repository;


@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
	 List<Order> findByCustomerId(String customerId);
	 List<Order> findByStatus(Order.OrderStatus status);
}
