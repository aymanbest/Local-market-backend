package com.localmarket.main.dto.analytics.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class TransactionAnalyticsResponse {
    private BigDecimal totalTransactionVolume;
    private long completedTransactions;
    private long pendingTransactions;
    private List<TransactionDetails> transactions;
}

