package com.localmarket.main.service.user;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.HashSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.review.ReviewRepository;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.review.Review;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.user.GetAllUsersResponse;
import com.localmarket.main.dto.user.UsersPageResponse;
import com.localmarket.main.entity.producer.ProducerApplication;
import com.localmarket.main.repository.producer.ProducerApplicationRepository;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.entity.product.StockReservation;
import com.localmarket.main.repository.product.StockReservationRepository;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProducerApplicationRepository producerApplicationRepository;
    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    @Value("${app.admin.email}")
    private String defaultAdminEmail;

    
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
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));
            
        // Prevent deletion of default admin account
        if (user.getEmail().equals(defaultAdminEmail)) {
            throw new ApiException(ErrorType.OPERATION_NOT_ALLOWED, 
                "Cannot delete the default admin account");
        }
        
        // If trying to delete an admin, check if it's the last one
        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(ErrorType.OPERATION_NOT_ALLOWED, 
                    "Cannot delete the last admin account");
            }
        }

        // Handle producer applications
        Optional<ProducerApplication> producerApplication = producerApplicationRepository.findByCustomer(user);
        if (producerApplication.isPresent()) {
            producerApplicationRepository.delete(producerApplication.get());
            producerApplicationRepository.flush();
        }
        
        // Also check for any other applications JUST IN CASE :)
        List<ProducerApplication> allApplications = producerApplicationRepository.findAll().stream()
            .filter(app -> app.getCustomer() != null && app.getCustomer().getUserId().equals(userId))
            .collect(Collectors.toList());
        if (!allApplications.isEmpty()) {
            producerApplicationRepository.deleteAll(allApplications);
            producerApplicationRepository.flush();
        }

        // Handle products created by this user
        List<Product> products = productRepository.findByProducerUserId(userId);
        for (Product product : products) {
            
            // Remove stock reservations
            List<StockReservation> stockReservations = stockReservationRepository.findByProduct(product);
            if (!stockReservations.isEmpty()) {
                stockReservationRepository.deleteAll(stockReservations);
            }
            
            // Remove reviews for this product
            List<Review> productReviews = reviewRepository.findByProductProductId(product.getProductId());
            if (!productReviews.isEmpty()) {
                productReviews.forEach(review -> review.setProduct(null));
                reviewRepository.saveAll(productReviews);
            }
            
            // Remove categories
            product.setCategories(new HashSet<>());
            productRepository.save(product);
        }
        
        // Now delete all products
        if (!products.isEmpty()) {
            productRepository.deleteAll(products);
            productRepository.flush();
        }

        // Batch process orders
        List<Order> orders = orderRepository.findByCustomerUserId(userId);
        if (!orders.isEmpty()) {
            orders.forEach(order -> order.setCustomer(null));
            orderRepository.saveAll(orders);
        }

        // Batch process reviews
        List<Review> reviews = reviewRepository.findByCustomerUserId(userId);
        if (!reviews.isEmpty()) {
            reviews.forEach(review -> review.setCustomer(null));
            reviewRepository.saveAll(reviews);
        }

        // Now safe to delete the user
        userRepository.delete(user);
        userRepository.flush();
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

