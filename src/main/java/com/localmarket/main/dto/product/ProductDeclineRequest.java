package com.localmarket.main.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeclineRequest {
    @NotBlank(message = "Decline reason is required")
    private String reason;
} 