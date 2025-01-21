package com.localmarket.main.dto.user;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfo {
    private String email;
    private Long userId;
    private String role;
} 