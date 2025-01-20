package com.localmarket.main.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import com.localmarket.main.entity.payment.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
} 