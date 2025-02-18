package com.localmarket.main.dto.support;

import lombok.Data;

@Data
public class TicketMessageRequest {
    private String message;
    private boolean isInternalNote;
} 