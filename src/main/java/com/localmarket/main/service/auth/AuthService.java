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
import com.localmarket.main.dto.auth.AuthServiceResult;
import com.localmarket.main.service.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import com.localmarket.main.websocket.NotificationWebSocketHandler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import com.localmarket.main.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ResetCodeService resetCodeService;
    private final AuthenticationManager authenticationManager;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthServiceResult register(RegisterRequest request, String jwt) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(ErrorType.EMAIL_ALREADY_EXISTS, "Email already exists");
        }

        Role roleToAssign = Role.CUSTOMER;
        boolean isAdminCreating = false;

        // Check if admin is trying to assign a role
        if (request.getRole() != null && request.getRole() != Role.CUSTOMER) {
            if (jwt == null || !"ADMIN".equals(jwtService.extractRole(jwt))) {
                throw new ApiException(ErrorType.UNAUTHORIZED_ROLE_ASSIGNMENT, 
                    "Admin authorization required to assign non-customer roles");
            }
            roleToAssign = request.getRole();
            isAdminCreating = true;
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
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", savedUser.getEmail(), e.getMessage());
        }
        
        System.err.println("roleToAssign: " + roleToAssign + "email: " + savedUser.getEmail());
        // Only store token if it's a regular customer registration (not admin creating)
        if (!isAdminCreating) {
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

    @Transactional
    public AuthServiceResult login(AuthRequest request) {
        try {
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            User user = new User();
            user.setUserId(userDetails.getId());
            user.setTokenVersion((userDetails.getTokenVersion() + 1) % 10);
            user.setLastLogin(LocalDateTime.now());
            user.setRole(userDetails.getRole());
            
            userRepository.updateTokenVersionAndLastLogin(
                user.getUserId(), 
                user.getTokenVersion(), 
                user.getLastLogin()
            );

            String token = jwtService.generateToken(user);
            tokenRepository.storeToken(token, user.getUserId());

            return new AuthServiceResult(
                AuthResponse.builder()
                    .status(200)
                    .message("Login successful")
                    .build(),
                token
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public void logout(String token, String userEmail) {
        Long userId = jwtService.extractUserId(token);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND,
                        "User with id " + userId + " not found"));

        user.setTokenVersion((user.getTokenVersion() + 1) % 10);
        userRepository.save(user);
        tokenRepository.invalidateToken(token);

        // Close any active WebSocket sessions for this user
        try {
            webSocketHandler.closeUserSessions(userEmail);
        } catch (Exception e) {
            log.warn("Failed to close WebSocket session for user {}: {}", userEmail, e.getMessage());
        }

        // Clear the SecurityContext
        SecurityContextHolder.clearContext();
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "No account found with this email"));

        // Generate reset code
        String resetCode = resetCodeService.generateCode(email);
        
        // Send email with reset code
        try {
            Map<String, Object> templateVariables = new HashMap<String, Object>();
            templateVariables.put("resetCode", resetCode);
            
            emailService.sendHtmlEmail(
                email,
                "Password Reset Code",
                user.getFirstname(),
                "password-reset-email",
                null,
                templateVariables
            );
        } catch (Exception e) {
            throw new ApiException(ErrorType.EMAIL_SENDING_FAILED, 
                "Failed to send reset code email");
        }
    }

    public void verifyAndResetPassword(String email, String code, String newPassword) {
        if (!resetCodeService.verifyCode(email, code)) {
            throw new ApiException(ErrorType.INVALID_TOKEN, 
                "Invalid or expired reset code");
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "User not found"));

        // Validate new password
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new ApiException(ErrorType.INVALID_PASSWORD, 
                "New password must be at least 6 characters long");
        }

        // Update password and invalidate existing tokens
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() + 1) % 10);
        userRepository.save(user);
    }
}
