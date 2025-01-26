package com.localmarket.main.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionDetails {
    private Long orderId;
    private String transactionId;
    private String customerName;
    private String producerName;
    private BigDecimal amount;
    private String status;
    private LocalDateTime date;
} 