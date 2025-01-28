package com.localmarket.main.dto.notification;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private String type;
    private String message;
    private Object data;
    private LocalDateTime timestamp;
} 