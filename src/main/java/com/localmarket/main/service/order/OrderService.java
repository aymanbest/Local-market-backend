package com.localmarket.main.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.repository.payment.PaymentRepository;

import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderItem;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.payment.Payment;

import com.localmarket.main.dto.order.OrderRequest;
import com.localmarket.main.dto.order.OrderItemRequest;
import com.localmarket.main.dto.account.RegisterRequest;
import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.payment.PaymentResponse;

import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.service.payment.PaymentService;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public Order createOrder(OrderRequest request, String userEmail) {
        Order order = new Order();
        
        // Handle user authentication/creation
        if (userEmail == null) {
            if (request.getAccountCreation() != null && request.getAccountCreation().isCreateAccount()) {
                // Create new account and authenticate
                RegisterRequest registerRequest = new RegisterRequest();
                registerRequest.setEmail(request.getGuestEmail());
                registerRequest.setUsername(request.getAccountCreation().getUsername());
                registerRequest.setPassword(request.getAccountCreation().getPassword());
                AuthResponse authResponse = authService.register(registerRequest);
                
                User newUser = userRepository.findByEmail(authResponse.getEmail())
                    .orElseThrow(() -> new RuntimeException("User creation failed"));
                order.setCustomer(newUser);
            } else {
                order.setGuestEmail(request.getGuestEmail());
            }
        } else {
            User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            order.setCustomer(customer);
        }

        // Calculate total price and process payment first
        BigDecimal totalPrice = calculateTotalPrice(request.getItems());
        PaymentResponse paymentResponse = paymentService.processPayment(request.getPaymentInfo(), totalPrice);
        Payment payment = paymentRepository.findById(paymentResponse.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Payment not found"));

        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
                
            // Check if producer is trying to order their own product
            if (userEmail != null && product.getProducer().getEmail().equals(userEmail)) {
                throw new RuntimeException("Producers cannot order their own products");
            }
            
            // Check stock availability
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            
            // Update product quantity
            product.setQuantity(product.getQuantity() - itemRequest.getQuantity());
            productRepository.save(product);
            
            orderItems.add(orderItem);
        }
        
        order.setTotalPrice(totalPrice);
        order.setItems(orderItems);
        
        // First save the order without payment
        Order savedOrder = orderRepository.save(order);

        // Update payment with orderId and save again
        payment.setOrderId(savedOrder.getOrderId());
        payment = paymentRepository.save(payment);

        // Update order with payment and save final version
        savedOrder.setPayment(payment);
        return orderRepository.save(savedOrder);
    }

    public List<Order> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByCustomer(user);
    }

    public Order getOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
            
        if (userEmail != null) {
            if (order.getCustomer() != null && !order.getCustomer().getEmail().equals(userEmail)) {
                throw new RuntimeException("Access denied");
            }
        } else if (order.getGuestEmail() == null) {
            throw new RuntimeException("Access denied");
        }
        
        return order;
    }

    private BigDecimal calculateTotalPrice(List<OrderItemRequest> items) {
        return items.stream()
            .map(item -> {
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
                return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
} 