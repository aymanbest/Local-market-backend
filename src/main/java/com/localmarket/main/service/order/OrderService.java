package com.localmarket.main.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

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
import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.payment.PaymentResponse;
import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.service.auth.TokenService;
import com.localmarket.main.service.payment.PaymentService;
import com.localmarket.main.dto.order.OrderResponse;
import com.localmarket.main.entity.payment.PaymentMethod;
import com.localmarket.main.dto.user.UserInfo;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;


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
    private final TokenService tokenService;

    public Order createOrder(OrderRequest request, UserInfo userInfo) {
        validateOrderStock(request.getItems());
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
            .orElseThrow(() -> new ApiException(ErrorType.PAYMENT_NOT_FOUND, "Payment not found"));

        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        
        List<OrderItem> orderItems = new ArrayList<>();
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
                
            // Check if producer is trying to order their own product
            if (userInfo != null && product.getProducer().getEmail().equals(userInfo.getEmail())) {
                throw new ApiException(ErrorType.ACCESS_DENIED, "Producers cannot order their own products");
            }
            
            // Check stock availability
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new ApiException(ErrorType.INSUFFICIENT_STOCK, "Insufficient stock for product: " + product.getName());
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

    public Order getOrder(Long orderId, String userEmail, String guestToken) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(ErrorType.ORDER_NOT_FOUND, 
                "Order with id " + orderId + " not found"));
            
        boolean hasAccess = false;
        
        // Check customer access
        if (userEmail != null && order.getCustomer() != null) {
            if (order.getCustomer().getEmail().equals(userEmail)) {
                hasAccess = true;
            }
        }
        
        // Check guest token access
        if (guestToken != null) {
            if (validateGuestAccess(order, guestToken)) {
                hasAccess = true;
            }
        }
        
        if (!hasAccess) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Access denied");
        }
        
        return order;
    }

    public List<Order> getUserOrdersByStatus(Long userId, OrderStatus status) {
        return orderRepository.findByCustomerUserIdAndStatus(userId, status);
    }

    public Order getUserOrder(Long orderId, Long userId) {
        return orderRepository.findByOrderIdAndCustomerUserId(orderId, userId)
            .orElseThrow(() -> new ApiException(ErrorType.ORDER_NOT_FOUND, 
                "Order not found or unauthorized"));
    }

    private BigDecimal calculateTotalPrice(List<OrderItemRequest> items) {
        return items.stream()
            .map(item -> {
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
                return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public OrderResponse createPendingOrder(OrderRequest request, String userEmail) {
        validateOrderStock(request.getItems());
        Order order = new Order();
        String accessToken = null;
        
        // Always generate guest token regardless of user status
        String guestToken = tokenService.generateGuestToken(request.getGuestEmail());
        order.setGuestToken(guestToken);
        order.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        if (userEmail == null) {
            order.setGuestEmail(request.getGuestEmail());
            accessToken = guestToken;
        } else {
            User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "User not found"));
            order.setCustomer(customer);
        }
        
        // Handle account creation if requested
        if (request.getAccountCreation() != null && request.getAccountCreation().isCreateAccount()) {
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(request.getGuestEmail());
            registerRequest.setUsername(request.getAccountCreation().getUsername());
            registerRequest.setPassword(request.getAccountCreation().getPassword());
            registerRequest.setFirstname(request.getAccountCreation().getFirstname());
            registerRequest.setLastname(request.getAccountCreation().getLastname());
            AuthResponse authResponse = authService.register(registerRequest, null);
            User newCustomer = userRepository.findByEmail(request.getGuestEmail())
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User creation failed"));
                
            order.setCustomer(newCustomer);
            // Don't clear guest email and token
            accessToken = authResponse.getToken();
        }
        
        // Rest of the order creation logic remains the same
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
            .accessToken(accessToken)
            .build();
    }

    @Transactional
    public Order processOrderPayment(Long orderId, PaymentInfo paymentInfo, String userEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(ErrorType.ORDER_NOT_FOUND, "Order not found: " + orderId));
            
        // Verify order ownership
        if (!canAccessOrder(order, userEmail)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Access denied");
        }

        // Validate payment info based on method
        validatePaymentInfo(paymentInfo);
        
        // Process payment
        PaymentResponse paymentResponse = paymentService.processPayment(paymentInfo, order.getTotalPrice());
        
        // Update order with payment info
        Payment payment = paymentRepository.findById(paymentResponse.getPaymentId())
            .orElseThrow(() -> new ApiException(ErrorType.PAYMENT_NOT_FOUND, "Payment not found"));
        payment.setOrderId(order.getOrderId());
        order.setPayment(payment);
        order.setStatus(OrderStatus.ACCEPTED);
        
        return orderRepository.save(order);
    }

    private void validatePaymentInfo(PaymentInfo paymentInfo) {
        if (paymentInfo.getPaymentMethod() == PaymentMethod.CARD) {
            if (paymentInfo.getCardNumber() == null || paymentInfo.getCardHolderName() == null || 
                paymentInfo.getExpiryDate() == null || paymentInfo.getCvv() == null) {
                throw new ApiException(ErrorType.VALIDATION_FAILED, "Invalid card payment details");
            }
        } else if (paymentInfo.getPaymentMethod() == PaymentMethod.BITCOIN) {
            if (paymentInfo.getTransactionHash() == null) {
                throw new ApiException(ErrorType.VALIDATION_FAILED, "Transaction hash is required for Bitcoin payments");
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
                .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
                
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    private boolean validateGuestAccess(Order order, String guestToken) {
        if (order.getGuestToken() == null || !order.getGuestToken().equals(guestToken)) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return order.getExpiresAt() != null && now.isBefore(order.getExpiresAt());
    }

    private void validateOrderStock(List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, 
                    "Product not found"));
                    
            if (product.getQuantity() < item.getQuantity()) {
                throw new ApiException(ErrorType.INSUFFICIENT_STOCK, 
                    String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d", 
                        product.getName(), 
                        product.getQuantity(), 
                        item.getQuantity()));
            }
        }
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long producerId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(ErrorType.ORDER_NOT_FOUND, "Order not found"));

        // Verify that the producer owns at least one product in the order
        boolean hasAccess = order.getItems().stream()
            .anyMatch(item -> item.getProduct().getProducer().getUserId().equals(producerId));

        if (!hasAccess) {
            throw new ApiException(ErrorType.ACCESS_DENIED, 
                "You can only update status for orders containing your products");
        }

        // Validate status transitions
        validateStatusTransition(order.getStatus(), newStatus);
        
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid status transitions
        if (currentStatus == OrderStatus.PENDING) {
            if (newStatus != OrderStatus.ACCEPTED && newStatus != OrderStatus.DECLINED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Pending orders can only be accepted or declined");
            }
        } else if (currentStatus == OrderStatus.ACCEPTED) {
            if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Accepted orders can only be delivered or cancelled");
            }
        } else if (currentStatus == OrderStatus.DELIVERED) {
            if (newStatus != OrderStatus.RETURNED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Delivered orders can only be marked as returned");
            }
        } else if (currentStatus == OrderStatus.DECLINED || 
                   currentStatus == OrderStatus.CANCELLED || 
                   currentStatus == OrderStatus.RETURNED) {
            throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                "Cannot change status of orders that are declined, cancelled, or returned");
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getProducerOrders(Long producerId) {
        return orderRepository.findByItemsProductProducerUserId(producerId);
    }

    @Transactional(readOnly = true)
    public List<Order> getProducerOrdersByStatus(Long producerId, OrderStatus status) {
        return orderRepository.findByItemsProductProducerUserIdAndStatus(producerId, status);
    }

} 