package com.localmarket.main.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String firstname;
    private String role;
    private Long userId;
    private String email;
    private String username;
    private String lastname;
    private String applicationStatus;
} 