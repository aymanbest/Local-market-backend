package com.localmarket.main.dto.payment;

import com.localmarket.main.entity.payment.PaymentMethod;
import lombok.Data;

@Data
public class PaymentInfo {
    private PaymentMethod paymentMethod;
    private String transactionDetails;
}