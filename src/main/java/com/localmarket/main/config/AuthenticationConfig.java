package com.localmarket.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import java.util.Collections;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.repository.producer.ProducerApplicationRepository;
import com.localmarket.main.entity.user.Role;

@Configuration
@RequiredArgsConstructor
public class AuthenticationConfig {
    private final UserRepository userRepository;
    private final ProducerApplicationRepository applicationRepository;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(username -> {
            User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));
            
            String applicationStatus = user.getRole() == Role.CUSTOMER ?
                applicationRepository.findByCustomer(user)
                    .map(app -> app.getStatus().name())
                    .orElse("NO_APPLICATION") :
                null;

            return CustomUserDetails.builder()
                .id(user.getUserId())
                .username(user.getUsername())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())))
                .role(user.getRole())
                .tokenVersion(user.getTokenVersion())
                .applicationStatus(applicationStatus)
                .build();
        });
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
} 