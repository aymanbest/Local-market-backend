package com.localmarket.main.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.localmarket.main.entity.user.Role;
import lombok.Builder;
import lombok.Data;
import java.util.Collection;

@Data
@Builder
public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final String email;
    private final String username;
    private final String firstname;
    private final String lastname;
    private final Role role;
    private final Integer tokenVersion;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String applicationStatus;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
} 