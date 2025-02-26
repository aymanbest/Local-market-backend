package com.localmarket.main.dto.user;

import org.springframework.data.domain.Page;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class UsersPageResponse {
    private Page<GetAllUsersResponse> users;
    private long totalActiveAccounts;
    private long totalProducerAccounts;
    private long totalAdminAccounts;
} 