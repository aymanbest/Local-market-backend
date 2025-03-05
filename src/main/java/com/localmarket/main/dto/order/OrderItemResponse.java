package com.localmarket.main.dto.order;

import com.localmarket.main.entity.order.OrderItem;
import com.localmarket.main.entity.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal price;
    private Long producerId;
    private String producerName;

    public static OrderItemResponse fromOrderItem(OrderItem orderItem) {
        Product product = orderItem.getProduct();
        return OrderItemResponse.builder()
                .orderItemId(orderItem.getOrderItemId())
                .productId(product.getProductId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .producerId(product.getProducer().getUserId())
                .producerName(product.getProducer().getUsername())
                .build();
    }
}