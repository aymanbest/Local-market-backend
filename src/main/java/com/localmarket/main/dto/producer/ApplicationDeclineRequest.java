package com.localmarket.main.dto.producer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationDeclineRequest {
    @NotBlank(message = "Decline reason is required")
    private String reason;
} 