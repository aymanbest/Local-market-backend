package com.localmarket.main.dto.support;

import com.localmarket.main.entity.support.TicketPriority;
import lombok.Data;

@Data
public class CreateTicketRequest {
    private String subject;
    private String message;
    private TicketPriority priority;
} 