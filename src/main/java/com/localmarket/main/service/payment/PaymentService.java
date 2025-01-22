package com.localmarket.main.service.payment;

import com.localmarket.main.dto.payment.PaymentInfo;
import com.localmarket.main.dto.payment.PaymentResponse;
import com.localmarket.main.entity.payment.Payment;
import com.localmarket.main.entity.payment.PaymentStatus;
import com.localmarket.main.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentInfo paymentInfo, BigDecimal amount) {
        // Simulate payment processing
        boolean isSuccessful = true; // 90% success rate
        
        if (!isSuccessful) {
            throw new ApiException(ErrorType.PAYMENT_FAILED, 
                "Payment processing failed. Please try again.");
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentInfo.getPaymentMethod());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setAmount(amount);
        
        Payment savedPayment = paymentRepository.save(payment);
        
        return new PaymentResponse(savedPayment.getPaymentId(), savedPayment.getTransactionId());
    }

} 