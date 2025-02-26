package com.localmarket.main.service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.user.GetAllUsersResponse;
import com.localmarket.main.dto.user.UsersPageResponse;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    
    // all Users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "User with id " + id + " not found"));
    }

    // by Username
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "User with username " + username + " not found"));
    }

    // Delete 
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "User with id " + id + " not found"));
            
        // Prevent deletion of default admin account
        if (user.getEmail().equals("admin@localmarket.com")) {
            throw new ApiException(ErrorType.OPERATION_NOT_ALLOWED, 
                "Cannot delete the default admin account");
        }
        
        // Prevent deletion of the last admin
        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.findByRole(Role.ADMIN).size();
            if (adminCount <= 1) {
                throw new ApiException(ErrorType.OPERATION_NOT_ALLOWED, 
                    "Cannot delete the last admin user");
            }
        }
        
        userRepository.delete(user);
    }

    // filter by role
    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User updateUser(Long id, RegisterRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "User with id " + id + " not found"));
        
        // Update basic information
        user.setUsername(request.getUsername());
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());
        
        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        
        // Update role if provided and different
        if (request.getRole() != null && request.getRole() != user.getRole()) {
            user.setRole(request.getRole());
            // Increment token version to invalidate existing tokens
            user.setTokenVersion((user.getTokenVersion() + 1) % 10);
        }
        
        return userRepository.save(user);
    }

    public UsersPageResponse getUsers(Role role, Pageable pageable) {
        Page<User> users = (role != null) 
            ? userRepository.findByRole(role, pageable)
            : userRepository.findAll(pageable);
        
        Page<GetAllUsersResponse> userResponses = users.map(user -> new GetAllUsersResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstname(),
            user.getLastname(),
            user.getRole(),
            user.getCreatedAt(),
            user.getLastLogin()
        ));

        // Get counts for different types of accounts
        long totalActiveAccounts = userRepository.count();
        long totalProducerAccounts = userRepository.countByRole(Role.PRODUCER);
        long totalAdminAccounts = userRepository.countByRole(Role.ADMIN);

        return new UsersPageResponse(
            userResponses,
            totalActiveAccounts,
            totalProducerAccounts,
            totalAdminAccounts
        );
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ApiException(ErrorType.INVALID_CREDENTIALS, "Current password is incorrect");
        }

        // Validate new password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ApiException(ErrorType.INVALID_PASSWORD, "Password cannot be empty");
        }

        // Password strength validation
        int strengthScore = 0;
        
        // Check length >= 8
        if (newPassword.length() >= 8) strengthScore++;
        // Check for uppercase
        if (newPassword.matches(".*[A-Z].*")) strengthScore++;
        // Check for lowercase
        if (newPassword.matches(".*[a-z].*")) strengthScore++;
        // Check for numbers
        if (newPassword.matches(".*[0-9].*")) strengthScore++;
        // Check for special characters
        if (newPassword.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strengthScore++;

        if (strengthScore < 3) {
            throw new ApiException(ErrorType.INVALID_PASSWORD, 
                "Password is not strong enough. Password must meet at least 3 of the following criteria: " +
                "8+ characters, uppercase letter, lowercase letter, number, special character");
        }

        // Update password and increment token version to invalidate existing tokens
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() + 1) % 10);
        userRepository.save(user);
    }
}

