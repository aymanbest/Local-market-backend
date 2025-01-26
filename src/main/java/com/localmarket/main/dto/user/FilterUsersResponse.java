package com.localmarket.main.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterUsersResponse {
    private Long userId;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
}
