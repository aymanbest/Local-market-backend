package com.localmarket.main.dto.user;

import com.localmarket.main.entity.user.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String status;
    private String message;
    private User user;
} 