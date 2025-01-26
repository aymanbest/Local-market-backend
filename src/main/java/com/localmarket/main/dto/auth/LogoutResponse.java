package com.localmarket.main.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutResponse {
    private String status;
} 