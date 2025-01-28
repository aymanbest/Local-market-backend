package com.localmarket.main.dto.payment;

import com.localmarket.main.entity.payment.PaymentMethod;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private PaymentMethod paymentMethod;
    
    // Card payment fields
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
    
    // Bitcoin payment fields
    private String transactionHash;
    
    // Common fields
    private String currency;
}