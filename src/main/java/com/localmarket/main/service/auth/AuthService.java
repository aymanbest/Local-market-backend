package com.localmarket.main.service.auth;

import com.localmarket.main.dto.account.RegisterRequest;
import com.localmarket.main.dto.auth.AuthRequest;
import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import com.localmarket.main.entity.user.Role;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.repository.token.TokenRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(ErrorType.EMAIL_ALREADY_EXISTS, "Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.CUSTOMER);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);
        tokenRepository.storeToken(token, savedUser.getUserId());

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorType.INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        tokenRepository.storeToken(token, user.getUserId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(String token) {
        String jwt = token.substring(7);
        Long userId = jwtService.extractUserId(jwt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND,
                        "uSER with id " + userId + " not found"));

        // Increment token version and reset to 0 if it reaches 10
        int newVersion = (user.getTokenVersion() + 1) % 10;
        user.setTokenVersion(newVersion);
        userRepository.save(user);
    }
}