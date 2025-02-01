package com.localmarket.main.dto.user;

import com.localmarket.main.entity.user.Role;
import java.time.LocalDateTime;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllUsersResponse {
    private Long userId;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

}
