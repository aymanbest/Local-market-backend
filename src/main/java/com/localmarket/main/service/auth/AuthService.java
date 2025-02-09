package com.localmarket.main.service.auth;

import com.localmarket.main.dto.auth.AuthRequest;
import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.security.authentication.BadCredentialsException;
import com.localmarket.main.entity.user.Role;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.repository.token.TokenRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.localmarket.main.dto.auth.AuthServiceResult;
import com.localmarket.main.service.email.EmailService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthServiceResult register(RegisterRequest request, String jwt) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(ErrorType.EMAIL_ALREADY_EXISTS, "Email already exists");
        }

        Role roleToAssign = Role.CUSTOMER;

        // Check if admin is trying to assign a role
        if (request.getRole() != null && request.getRole() != Role.CUSTOMER) {
            if (jwt == null) {
                throw new ApiException(ErrorType.UNAUTHORIZED_ROLE_ASSIGNMENT, 
                    "Admin authorization required to assign non-customer roles");
            }

            String role = jwtService.extractRole(jwt);
            if (!"ADMIN".equals(role)) {
                throw new ApiException(ErrorType.UNAUTHORIZED_ROLE_ASSIGNMENT, 
                    "Only admins can assign non-customer roles");
            }

            roleToAssign = request.getRole();
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLastLogin(LocalDateTime.now());
        user.setRole(roleToAssign);
        
        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);
        
        // Send welcome email
        try {
            emailService.sendHtmlEmail(
                savedUser.getEmail(),
                "Welcome to LocalMarket!",
                savedUser.getFirstname(),
                "welcome-email",
                null,
                new HashMap<>()
            );
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
        }
        
        if (roleToAssign == Role.CUSTOMER) {
            tokenRepository.storeToken(token, savedUser.getUserId());
        }
        
        return new AuthServiceResult(
            AuthResponse.builder()
                .status(200)
                .message("Registration successful" + (roleToAssign != Role.CUSTOMER ? " with role: " + roleToAssign : ""))
                .build(),
            token
        );
    }

    public AuthServiceResult login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setTokenVersion((user.getTokenVersion() + 1) % 10);
        String token = jwtService.generateToken(user);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        tokenRepository.storeToken(token, user.getUserId());
        
        return new AuthServiceResult(
            AuthResponse.builder()
                .status(200)
                .message("Login successful")
                .build(),
            token
        );
    }

    @Transactional
    public void logout(String token) {
        Long userId = jwtService.extractUserId(token);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND,
                        "User with id " + userId + " not found"));

        user.setTokenVersion((user.getTokenVersion() + 1) % 10);
        userRepository.save(user);
        tokenRepository.invalidateToken(token);
    }
}
