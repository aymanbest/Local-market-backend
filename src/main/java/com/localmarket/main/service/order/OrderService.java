package com.localmarket.main.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.beans.factory.annotation.Value;

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
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.payment.PaymentResponse;
import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.service.auth.TokenService;
import com.localmarket.main.service.notification.customer.CustomerNotificationService;
import com.localmarket.main.service.notification.producer.ProducerNotificationService;
import com.localmarket.main.service.payment.PaymentService;
import com.localmarket.main.dto.order.OrderResponse;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.service.product.ProductService;
import com.localmarket.main.service.coupon.CouponService;
import com.localmarket.main.service.email.EmailService;
import com.localmarket.main.dto.coupon.CouponValidationResponse;
import com.localmarket.main.dto.order.OrderItemResponse;

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
    private final ProducerNotificationService producerNotificationService;
    private final ProductService productService;
    private final CustomerNotificationService customerNotificationService;
    private final CouponService couponService;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByCustomerUserId(userId);
    }

    public Order getOrder(Long orderId, String userEmail, String accessToken) {
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
        if (accessToken != null) {
            if (validateGuestAccess(order, accessToken)) {
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
    public List<OrderResponse> createPendingOrder(OrderRequest request, String userEmail) {
        log.info("Creating pending order. User email: {}", userEmail);
        
        validateOrderStock(request.getItems());
        
        // Group items by producer
        Map<Long, List<OrderItemRequest>> itemsByProducer = request.getItems().stream()
            .collect(Collectors.groupingBy(item -> {
                Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
                return product.getProducer().getUserId();
            }));
        
        List<OrderResponse> orders = new ArrayList<>();
        
        // Handle authentication and customer setup
        String accessToken;
        User customer = null;

        // If we have userEmail, treat as authenticated user
        if (userEmail != null) {
            log.info("Processing as authenticated user with email: {}", userEmail);
            customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));
            accessToken = tokenService.createCheckoutToken(userEmail);
        } else if (request.getGuestEmail() == null) {
            // Neither authenticated nor guest email provided
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Guest email is required for guest orders");
        } else {
            // Guest user flow
            log.info("Processing as guest user with email: {}", request.getGuestEmail());
            accessToken = tokenService.createCheckoutToken(request.getGuestEmail());
            if (request.getAccountCreation() != null && request.getAccountCreation().isCreateAccount()) {
                RegisterRequest registerRequest = createRegisterRequest(request);
                authService.register(registerRequest, null);
                customer = userRepository.findByEmail(request.getGuestEmail())
                    .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User creation failed"));
            }
        }
        
        List<Order> createdOrders = new ArrayList<>();
        
        // Create separate order for each producer
        for (Map.Entry<Long, List<OrderItemRequest>> entry : itemsByProducer.entrySet()) {
            Order order = new Order();
            
            // Set customer info using the same access token
            if (customer != null) {
                setupCustomerOrder(order, customer, accessToken);
            } else {
                setupGuestOrder(order, request, accessToken);
            }
            
            // Set common order details
            setupOrderDetails(order, request);
            
            // Create order items for this producer only
            List<OrderItem> orderItems = createOrderItems(order, entry.getValue());
            order.setItems(orderItems);
            
            // Calculate total for this producer's items
            BigDecimal totalPrice = calculateTotalPrice(entry.getValue());
            order.setTotalPrice(totalPrice);
            
            // Apply coupon discount
            if (request.getCouponCode() != null) {
                // Validate coupon first
                CouponValidationResponse validationResponse = couponService.validateCoupon(
                    request.getCouponCode(),
                    order.getTotalPrice(),
                    order.getCustomer() != null ? order.getCustomer().getUserId() : null
                );

                if (!validationResponse.isValid()) {
                    throw new ApiException(ErrorType.VALIDATION_FAILED, validationResponse.getMessage());
                }

                // Apply the validated coupon
                BigDecimal newTotal = validationResponse.getFinalPrice().setScale(2, BigDecimal.ROUND_HALF_UP);
                if (newTotal.precision() > 12) { // 10 digits + 2 decimal places
                    throw new ApiException(ErrorType.VALIDATION_FAILED, 
                        "Total price after discount exceeds maximum allowed digits");
                }
                
                order.setTotalPrice(newTotal);
                couponService.applyCoupon(request.getCouponCode(), order.getCustomer() != null ? 
                    order.getCustomer().getUserId() : null);
            }
            
            // Save order and reserve stock
            order = orderRepository.save(order);
            try {
                productService.reserveStock(order);
            } catch (ApiException e) {
                throw new ApiException(ErrorType.INSUFFICIENT_STOCK, e.getMessage());
            }
            
            // Notify producer
            producerNotificationService.notifyNewOrder(entry.getKey(), order);
            
            createdOrders.add(order);
            
            orders.add(OrderResponse.builder()
                .orderId(order.getOrderId())
                .guestEmail(order.getGuestEmail())
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream()
                    .map(OrderItemResponse::fromOrderItem)
                    .collect(Collectors.toList()))
                .accessToken(accessToken)
                .build());
        }
        
        // Send a single confirmation email for all orders if there are multiple orders
        if (createdOrders.size() > 1) {
            sendBundleOrderConfirmationEmail(createdOrders, accessToken);
        } else if (createdOrders.size() == 1) {
            // Send individual confirmation for a single order
            sendOrderConfirmationEmail(createdOrders.get(0));
        }
        
        return orders;
    }

    private RegisterRequest createRegisterRequest(OrderRequest request) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(request.getGuestEmail());
        registerRequest.setUsername(request.getAccountCreation().getUsername());
        registerRequest.setPassword(request.getAccountCreation().getPassword());
        registerRequest.setFirstname(request.getAccountCreation().getFirstname());
        registerRequest.setLastname(request.getAccountCreation().getLastname());
        return registerRequest;
    }

    private void setupGuestOrder(Order order, OrderRequest request, String accessToken) {
        // Skip guest email validation if user is authenticated (userEmail is not null)
        if (request.getGuestEmail() == null && order.getCustomer() == null) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Guest email is required for guest orders");
        }
        
        if (order.getCustomer() == null) {
            order.setGuestEmail(request.getGuestEmail());
        }
        
        order.setAccessToken(accessToken);
        order.setExpiresAt(LocalDateTime.now().plusHours(24));
    }

    private void setupCustomerOrder(Order order, User customer, String accessToken) {
        order.setAccessToken(accessToken);
        order.setCustomer(customer);
        order.setExpiresAt(LocalDateTime.now().plusHours(24));
    }

    private void setupOrderDetails(Order order, OrderRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Phone number is required");
        }
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
    }

    @Transactional
    public List<Order> processOrdersPayment(PaymentInfo paymentInfo, String accessToken) {
        // First validate the access token
        String email = tokenService.validateAccessToken(accessToken);
        if (email == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid or expired access token");
        }
        
        // Get all orders from the checkout session
        List<Order> orders = orderRepository.findAllByAccessToken(accessToken);
        if (orders.isEmpty()) {
            throw new ApiException(ErrorType.ORDER_NOT_FOUND, "No orders found for this access token");
        }
        
        // Validate orders are in PENDING_PAYMENT status
        orders.forEach(order -> {
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new ApiException(ErrorType.INVALID_REQUEST, 
                    "Order " + order.getOrderId() + " is not pending payment");
            }
        });
        
        // Calculate total price for all orders
        BigDecimal totalAmount = orders.stream()
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Total order amount must be greater than 0");
        }
        
        try {
            // Process single payment for total amount
            PaymentResponse paymentResponse = paymentService.processPayment(paymentInfo, totalAmount);
            
            // Update all orders
            List<Order> processedOrders = new ArrayList<>();
            for (Order order : orders) {
                order.setStatus(OrderStatus.PAYMENT_COMPLETED);
                productService.confirmStockReduction(order);
                updateOrderPayment(order, paymentResponse);
                processedOrders.add(orderRepository.save(order));
            }
            
            return processedOrders;
        } catch (Exception e) {
            // If payment fails, update all orders
            orders.forEach(order -> {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                productService.releaseStock(order);
                orderRepository.save(order);
            });
            throw e;
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

    private boolean validateGuestAccess(Order order, String accessToken) {
        if (order.getAccessToken() == null || !order.getAccessToken().equals(accessToken)) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return order.getExpiresAt() != null && now.isBefore(order.getExpiresAt());
    }

    protected void validateOrderStock(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Cart is empty");
        }
        
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
        
        // If status is SHIPPED, send delivery notification
        if (newStatus == OrderStatus.SHIPPED) {
            customerNotificationService.notifyDeliveryUpdate(order, 
                "Your order has been shipped and is on its way!");
        } else if (newStatus == OrderStatus.DELIVERED) {
            customerNotificationService.notifyDeliveryUpdate(order, 
                "Your order has been delivered successfully!");
        } else {
            customerNotificationService.notifyOrderStatusUpdate(order);
        }
        
        return orderRepository.save(order);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid status transitions
        if (currentStatus == OrderStatus.PENDING_PAYMENT) {
            if (newStatus != OrderStatus.PAYMENT_COMPLETED && newStatus != OrderStatus.PAYMENT_FAILED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Pending payment orders can only be completed or failed");
            }
        } else if (currentStatus == OrderStatus.PAYMENT_COMPLETED) {
            if (newStatus != OrderStatus.PROCESSING && newStatus != OrderStatus.CANCELLED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Completed orders can only be processed or cancelled");
            }
        } else if (currentStatus == OrderStatus.PAYMENT_FAILED) {
            throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                "Cannot change status of orders that have failed payment");
        } else if (currentStatus == OrderStatus.PROCESSING) {
            if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.CANCELLED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Processing orders can only be shipped or cancelled");
            }
        } else if (currentStatus == OrderStatus.SHIPPED) {
            if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.CANCELLED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Shipped orders can only be delivered or cancelled");
            }
        } else if (currentStatus == OrderStatus.DELIVERED) {
            if (newStatus != OrderStatus.RETURNED) {
                throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                    "Delivered orders can only be marked as returned");
            }
        } else if (currentStatus == OrderStatus.CANCELLED || 
                   currentStatus == OrderStatus.RETURNED) {
            throw new ApiException(ErrorType.INVALID_STATUS_TRANSITION, 
                "Cannot change status of orders that are cancelled or returned");
        }
    }

    @Transactional(readOnly = true)
    public Page<Order> getProducerOrders(Long producerId, String customerEmail, Pageable pageable) {
        if (customerEmail != null && !customerEmail.isEmpty()) {
            // Use JPA-level filtering for customer email
            return orderRepository.findByProducerIdAndCustomerEmailContaining(
                producerId, customerEmail, pageable);
        } else {
            // If no customer email filter, use standard JPA pagination
            return orderRepository.findByItemsProductProducerUserId(producerId, pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<Order> getProducerOrdersByStatus(Long producerId, OrderStatus status, String customerEmail, Pageable pageable) {
        if (customerEmail != null && !customerEmail.isEmpty()) {
            // Use JPA-level filtering for customer email and status
            return orderRepository.findByProducerIdAndStatusAndCustomerEmailContaining(
                producerId, status, customerEmail, pageable);
        } else {
            // If no customer email filter, use standard JPA pagination
            return orderRepository.findByItemsProductProducerUserIdAndStatus(producerId, status, pageable);
        }
    }

    private void updateOrderPayment(Order order, PaymentResponse paymentResponse) {
        // Get the original payment
        Payment originalPayment = paymentRepository.findById(paymentResponse.getPaymentId())
            .orElseThrow(() -> new ApiException(ErrorType.PAYMENT_NOT_FOUND, "Payment not found"));
        
        // For multiple orders, create a copy of the payment for each order
        if (order.getOrderId() != null) {
            // Check if this is not the first order associated with the payment
            if (originalPayment.getOrderId() != null && !originalPayment.getOrderId().equals(order.getOrderId())) {
                // Create a new payment entry for this order
                Payment paymentCopy = new Payment();
                paymentCopy.setAmount(originalPayment.getAmount());
                paymentCopy.setPaymentMethod(originalPayment.getPaymentMethod());
                paymentCopy.setPaymentStatus(originalPayment.getPaymentStatus());
                paymentCopy.setTransactionId(originalPayment.getTransactionId() + "-" + order.getOrderId());
                paymentCopy.setCreatedAt(LocalDateTime.now());
                paymentCopy.setOrderId(order.getOrderId());
                
                // Save the copy and associate it with this order
                Payment savedPayment = paymentRepository.save(paymentCopy);
                order.setPayment(savedPayment);
                return;
            }
        }
        
        // For the first order or if there's only one order
        originalPayment.setOrderId(order.getOrderId());
        order.setPayment(originalPayment);
    }

    public Order getGuestOrders(String accessToken) {
        String email = tokenService.validateAccessToken(accessToken);
        if (email == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid or expired access token");
        }
        return orderRepository.findByAccessToken(accessToken)
            .orElseThrow(() -> new ApiException(ErrorType.ORDER_NOT_FOUND, "Order not found"));
    }

    public Order getOrderByAccessToken(String accessToken) {
        String email = tokenService.validateAccessToken(accessToken);
        if (email == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid or expired access token");
        }
        
        Order order = orderRepository.findByAccessToken(accessToken)
            .orElseThrow(() -> new ApiException(ErrorType.ORDER_NOT_FOUND, 
                "Order not found for this access token"));
                
        if (order.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorType.TOKEN_EXPIRED, "Access token has expired");
        }
        
        return order;
    }

    public List<Order> getOrdersByAccessToken(String accessToken) {
        String email = tokenService.validateAccessToken(accessToken);
        if (email == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid or expired access token");
        }
        
        List<Order> orders = orderRepository.findAllByAccessToken(accessToken);
        if (orders.isEmpty()) {
            throw new ApiException(ErrorType.ORDER_NOT_FOUND, "No orders found for this access token");
        }
        
        // Check expiration on the first order since all orders in a checkout session share the same expiration
        // if (orders.get(0).getExpiresAt().isBefore(LocalDateTime.now())) {
        //     throw new ApiException(ErrorType.TOKEN_EXPIRED, "Access token has expired");
        // }
        
        return orders;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderBundle(String accessToken) {
        List<Order> orders = orderRepository.findAllByAccessToken(accessToken);
        if (orders.isEmpty()) {
            throw new ApiException(ErrorType.ORDER_NOT_FOUND, 
                "No orders found for this access token bundle");
        }
        return orders.stream()
            .map(OrderResponse::fromOrder)
            .collect(Collectors.toList());
    }

    private void sendOrderConfirmationEmail(Order order) {
        String recipientEmail = order.getCustomer() != null ? 
            order.getCustomer().getEmail() : order.getGuestEmail();
        String recipientName = order.getCustomer() != null ? 
            order.getCustomer().getFirstname() : "Valued Customer";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("name", recipientName);
        templateModel.put("orderId", order.getOrderId());
        templateModel.put("items", order.getItems().stream()
            .map(item -> Map.of(
                "productName", item.getProduct().getName(),
                "quantity", item.getQuantity(),
                "price", item.getProduct().getPrice(),
                "imageUrl", getFullImageUrl(item.getProduct().getImageUrl())
            ))
            .collect(Collectors.toList()));
        
        BigDecimal originalTotal = order.getTotalPrice();
        BigDecimal finalTotal = order.getTotalPrice();
        BigDecimal discount = BigDecimal.ZERO;
        
        // Calculate discount if coupon was applied
        if (order.getTotalPrice().compareTo(originalTotal) < 0) {
            discount = originalTotal.subtract(order.getTotalPrice());
            finalTotal = order.getTotalPrice();
        }

        templateModel.put("subtotal", originalTotal);
        templateModel.put("shipping", BigDecimal.ZERO);
        templateModel.put("discount", discount);
        templateModel.put("total", finalTotal);
        templateModel.put("shippingAddress", order.getShippingAddress());
        templateModel.put("isBundle", false);
        
        // Add bundle link if there's an access token
        if (order.getAccessToken() != null) {
            templateModel.put("bundleLink", frontendUrl + "/orders/bundle/" + order.getAccessToken());
            templateModel.put("hasBundle", true);
        } else {
            templateModel.put("hasBundle", false);
        }

        try {
            emailService.sendHtmlEmail(
                recipientEmail,
                "Order Confirmation - LocalMarket",
                recipientName,
                "receipt_email",
                null,
                templateModel
            );
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: {}", e.getMessage());
        }
    }
    
    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "https://placehold.co/600x400?text=No+Image";
        }
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        
        // Extract the base URL from the frontend URL (remove the path part)
        String baseUrl = frontendUrl.replaceAll("(https?://[^/]+).*", "$1");
        // Replace frontend port (typically 3000, 5173, etc.) with backend port (8080)
        String backendUrl = baseUrl.replace(":5173", ":8080").replace(":3000", ":8080");
        
        return backendUrl + imageUrl;
    }

    @Transactional(readOnly = true)
    public Page<Order> getUserOrdersPaginated(Long userId, Pageable pageable) {
        return orderRepository.findByCustomerUserId(userId, pageable);
    }

    private void sendBundleOrderConfirmationEmail(List<Order> orders, String accessToken) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        
        // Use the first order to get customer information
        Order firstOrder = orders.get(0);
        String recipientEmail = firstOrder.getCustomer() != null ? 
            firstOrder.getCustomer().getEmail() : firstOrder.getGuestEmail();
        String recipientName = firstOrder.getCustomer() != null ? 
            firstOrder.getCustomer().getFirstname() : "Valued Customer";

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("name", recipientName);
        
        // Create a list of all items from all orders
        List<Map<String, Object>> allItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                allItems.add(Map.of(
                    "productName", item.getProduct().getName(),
                    "quantity", item.getQuantity(),
                    "price", item.getProduct().getPrice(),
                    "imageUrl", getFullImageUrl(item.getProduct().getImageUrl()),
                    "producerName", item.getProduct().getProducer().getUsername()
                ));
            }
            totalAmount = totalAmount.add(order.getTotalPrice());
        }
        
        templateModel.put("items", allItems);
        templateModel.put("subtotal", totalAmount);
        templateModel.put("shipping", BigDecimal.ZERO);
        templateModel.put("discount", BigDecimal.ZERO);
        templateModel.put("total", totalAmount);
        templateModel.put("shippingAddress", firstOrder.getShippingAddress());
        templateModel.put("bundleLink", frontendUrl + "/orders/bundle/" + accessToken);
        templateModel.put("hasBundle", true);
        templateModel.put("isBundle", true);
        templateModel.put("orderCount", orders.size());
        
        // Add list of order IDs
        List<Long> orderIds = orders.stream()
            .map(Order::getOrderId)
            .collect(Collectors.toList());
        templateModel.put("orderIds", orderIds);

        try {
            emailService.sendHtmlEmail(
                recipientEmail,
                "Order Confirmation - LocalMarket",
                recipientName,
                "receipt_email",
                null,
                templateModel
            );
        } catch (Exception e) {
            log.error("Failed to send bundle order confirmation email: {}", e.getMessage());
        }
    }

} 