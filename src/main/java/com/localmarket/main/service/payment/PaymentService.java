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
import com.localmarket.main.entity.payment.PaymentMethod;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentResponse processPayment(PaymentInfo paymentInfo, BigDecimal amount) {
        // Validate payment info based on method
        validatePaymentInfo(paymentInfo);
        
        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentInfo.getPaymentMethod());
        payment.setTransactionId("TRANS_" + UUID.randomUUID().toString());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setAmount(amount); // Amount comes from order
        Payment savedPayment = paymentRepository.save(payment);
        
        return new PaymentResponse(savedPayment.getPaymentId(), savedPayment.getTransactionId());
    }

    private void validatePaymentInfo(PaymentInfo paymentInfo) {
        if (paymentInfo.getPaymentMethod() == PaymentMethod.CARD) {
            if (paymentInfo.getCardNumber() == null || paymentInfo.getCardHolderName() == null || 
                paymentInfo.getExpiryDate() == null || paymentInfo.getCvv() == null) {
                throw new ApiException(ErrorType.VALIDATION_FAILED, "Invalid card payment details");
            }
        } else if (paymentInfo.getPaymentMethod() == PaymentMethod.BITCOIN) {
            if (paymentInfo.getTransactionHash() == null) {
                throw new ApiException(ErrorType.VALIDATION_FAILED, "Transaction hash is required for Bitcoin payments");
            }
        }
    }
} 