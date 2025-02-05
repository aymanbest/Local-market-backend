package com.localmarket.main.dto.order;

import lombok.Data;
import com.localmarket.main.dto.user.AccountCreationRequest;
import java.util.List;
import com.localmarket.main.entity.payment.PaymentMethod;

@Data
public class OrderRequest {
    private List<OrderItemRequest> items;
    private String shippingAddress;
    private String phoneNumber;
    private String guestEmail;
    private AccountCreationRequest accountCreation;
    private PaymentMethod paymentMethod;
    private String couponCode;
}
