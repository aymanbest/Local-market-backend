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
import com.localmarket.main.entity.order.OrderStatus;

import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderItem;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.payment.Payment;
import com.localmarket.main.dto.payment.PaymentInfo;
import com.localmarket.main.dto.order.OrderRequest;
import com.localmarket.main.dto.order.OrderItemRequest;
import com.localmarket.main.dto.account.RegisterRequest;
import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.payment.PaymentResponse;
import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.service.payment.PaymentService;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.dto.order.OrderResponse;
import com.localmarket.main.entity.payment.PaymentMethod;
import com.localmarket.main.dto.user.UserInfo;


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

    public Order createOrder(OrderRequest request, UserInfo userInfo) {
        Order order = new Order();
        
        if (userInfo != null) {
            User customer = userRepository.getReferenceById(userInfo.getUserId());
            order.setCustomer(customer);
        } else {
            order.setGuestEmail(request.getGuestEmail());
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
            if (userInfo != null && product.getProducer().getEmail().equals(userInfo.getEmail())) {
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

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByCustomerUserId(userId);
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

    public List<Order> getUserOrdersByStatus(Long userId, OrderStatus status) {
        return orderRepository.findByCustomerUserIdAndStatus(userId, status);
    }

    public Order getUserOrder(Long orderId, Long userId) {
        return orderRepository.findByOrderIdAndCustomerUserId(orderId, userId)
            .orElseThrow(() -> new RuntimeException("Order not found or unauthorized"));
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

    @Transactional
    public OrderResponse createPendingOrder(OrderRequest request, String userEmail) {
        Order order = new Order();
        String token = null;
        
        // Set customer or guest email
        if (userEmail != null) {
            User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            order.setCustomer(customer);
        } else if (request.getGuestEmail() != null && request.getShippingAddress() != null) {
            order.setGuestEmail(request.getGuestEmail());
        } else {
            throw new RuntimeException("Either user token or guest email with shipping address is required");
        }
        
        // Handle account creation if requested
        if (request.getAccountCreation() != null && request.getAccountCreation().isCreateAccount()) {
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(request.getGuestEmail());
            registerRequest.setUsername(request.getAccountCreation().getUsername());
            registerRequest.setPassword(request.getAccountCreation().getPassword());
            registerRequest.setRole(Role.CUSTOMER);
            
            AuthResponse authResponse = authService.register(registerRequest);
            User newCustomer = userRepository.findByEmail(request.getGuestEmail())
                .orElseThrow(() -> new RuntimeException("User creation failed"));
                
            order.setCustomer(newCustomer);
            order.setGuestEmail(null);
            token = authResponse.getToken();
        }
        
        // Rest of the method remains the same
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setStatus(OrderStatus.PENDING);
        
        BigDecimal totalPrice = calculateTotalPrice(request.getItems());
        order.setTotalPrice(totalPrice);
        order.setItems(createOrderItems(order, request.getItems()));
        
        Order savedOrder = orderRepository.save(order);
        
        return OrderResponse.builder()
            .orderId(savedOrder.getOrderId())
            .order(savedOrder)
            .token(token)
            .build();
    }

    @Transactional
    public Order processOrderPayment(Long orderId, PaymentInfo paymentInfo, String userEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
        // Verify order ownership
        if (!canAccessOrder(order, userEmail)) {
            throw new RuntimeException("Access denied");
        }

        // Validate payment info based on method
        validatePaymentInfo(paymentInfo);
        
        // Process payment
        PaymentResponse paymentResponse = paymentService.processPayment(paymentInfo, order.getTotalPrice());
        
        // Update order with payment info
        Payment payment = paymentRepository.findById(paymentResponse.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setOrderId(order.getOrderId());
        order.setPayment(payment);
        order.setStatus(OrderStatus.ACCEPTED);
        
        return orderRepository.save(order);
    }

    private void validatePaymentInfo(PaymentInfo paymentInfo) {
        if (paymentInfo.getPaymentMethod() == PaymentMethod.CARD) {
            if (paymentInfo.getCardNumber() == null || paymentInfo.getCardHolderName() == null || 
                paymentInfo.getExpiryDate() == null || paymentInfo.getCvv() == null) {
                throw new RuntimeException("Invalid card payment details");
            }
        } else if (paymentInfo.getPaymentMethod() == PaymentMethod.BITCOIN) {
            if (paymentInfo.getTransactionHash() == null) {
                throw new RuntimeException("You didnt provide a transaction hash");
            }
        }
    }

    private boolean canAccessOrder(Order order, String userEmail) {
        if (userEmail != null) {
            // Registered user access
            return order.getCustomer() != null && 
                   order.getCustomer().getEmail().equals(userEmail);
        } else {
            // Guest access - allow if the order has a guest email
            return order.getGuestEmail() != null;
        }
    }

    private List<OrderItem> createOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
                
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);
        }
        return orderItems;
    }
} 