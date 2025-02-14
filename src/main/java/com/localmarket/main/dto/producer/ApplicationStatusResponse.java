package com.localmarket.main.dto.producer;

import com.localmarket.main.entity.producer.ApplicationStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationStatusResponse {
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    private String declineReason;
} 