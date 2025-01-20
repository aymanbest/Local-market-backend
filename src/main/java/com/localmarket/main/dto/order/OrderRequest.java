package com.localmarket.main.dto.order;

import lombok.Data;
import com.localmarket.main.dto.payment.PaymentInfo;
import com.localmarket.main.dto.user.AccountCreationRequest;
import java.util.List;


@Data
public class OrderRequest {
    private List<OrderItemRequest> items;
    private String shippingAddress;
    private String phoneNumber;
    private String guestEmail;
    private PaymentInfo paymentInfo;
    private AccountCreationRequest accountCreation;
}
