package com.localmarket.main.dto.support;

import com.localmarket.main.entity.user.Role;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long userId;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private Role role;
    private Integer tokenVersion;
    private LocalDateTime lastLogin;
} 