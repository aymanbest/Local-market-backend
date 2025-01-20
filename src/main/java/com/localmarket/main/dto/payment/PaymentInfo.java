package com.localmarket.main.dto.payment;

import com.localmarket.main.entity.payment.PaymentMethod;
import com.localmarket.main.dto.user.AccountCreationRequest;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private PaymentMethod paymentMethod;
    private String transactionDetails;
    
    // Card payment fields
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
    
    // Bitcoin payment fields
    private String transactionHash;
    
    // Common fields
    private BigDecimal amount;
    private String currency;
    private AccountCreationRequest accountCreation;
}