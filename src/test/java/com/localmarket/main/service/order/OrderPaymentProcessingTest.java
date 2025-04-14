package com.localmarket.main.service.order;

import com.localmarket.main.dto.payment.PaymentInfo;
import com.localmarket.main.dto.payment.PaymentResponse;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.entity.payment.Payment;
import com.localmarket.main.entity.payment.PaymentMethod;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.payment.PaymentRepository;
import com.localmarket.main.service.payment.PaymentService;
import com.localmarket.main.service.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import com.localmarket.main.entity.payment.PaymentStatus;
import com.localmarket.main.service.auth.TokenService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OrderPaymentProcessingTest {

    private OrderRepository orderRepository;
    private PaymentService paymentService;
    private PaymentRepository paymentRepository;
    private ProductService productService;
    private OrderService orderService;
    private TokenService tokenService;
    
    private String accessToken;
    private List<Order> orders;
    private PaymentInfo paymentInfo;
    private PaymentResponse paymentResponse;
    
    @BeforeEach
    void setUp() {
        // Initialize mocks with lenient stubs
        MockitoAnnotations.openMocks(this);
        
        // Create mock instances with lenient settings
        orderRepository = Mockito.mock(OrderRepository.class, withSettings().lenient());
        paymentService = Mockito.mock(PaymentService.class, withSettings().lenient());
        paymentRepository = Mockito.mock(PaymentRepository.class, withSettings().lenient());
        productService = Mockito.mock(ProductService.class, withSettings().lenient());
        tokenService = Mockito.mock(com.localmarket.main.service.auth.TokenService.class, withSettings().lenient());
        
        // We need to create other dependencies for OrderService constructor
        // Just create stub versions of the other dependencies that aren't used in our tests
        orderService = new OrderService(
            orderRepository,
            Mockito.mock(com.localmarket.main.repository.product.ProductRepository.class, withSettings().lenient()),
            Mockito.mock(com.localmarket.main.repository.user.UserRepository.class, withSettings().lenient()),
            Mockito.mock(com.localmarket.main.service.auth.AuthService.class, withSettings().lenient()),
            paymentService,
            paymentRepository,
            tokenService,
            Mockito.mock(com.localmarket.main.service.notification.producer.ProducerNotificationService.class, withSettings().lenient()),
            productService,
            Mockito.mock(com.localmarket.main.service.notification.customer.CustomerNotificationService.class, withSettings().lenient()),
            Mockito.mock(com.localmarket.main.service.coupon.CouponService.class, withSettings().lenient()),
            Mockito.mock(com.localmarket.main.service.email.EmailService.class, withSettings().lenient())
        );
        
        accessToken = "test-access-token";
        
        // Mock the token validation to return a valid email for our test token
        when(tokenService.validateAccessToken(accessToken)).thenReturn("valid@example.com");
        
        // Create payment info
        paymentInfo = PaymentInfo.builder()
                .paymentMethod(PaymentMethod.CARD)
                .cardNumber("4111111111111111")
                .cardHolderName("John Doe")
                .expiryDate("12/25")
                .cvv("123")
                .currency("USD")
                .build();
        
        // Create payment response - PaymentResponse takes paymentId and transactionId parameters
        paymentResponse = new PaymentResponse(1L, "txn_12345");
        
        // Create orders
        orders = new ArrayList<>();
        
        Order order1 = new Order();
        order1.setOrderId(1L);
        order1.setStatus(OrderStatus.PENDING_PAYMENT);
        order1.setTotalPrice(new BigDecimal("25.00"));
        order1.setAccessToken(accessToken);
        
        Order order2 = new Order();
        order2.setOrderId(2L);
        order2.setStatus(OrderStatus.PENDING_PAYMENT);
        order2.setTotalPrice(new BigDecimal("35.00"));
        order2.setAccessToken(accessToken);
        
        orders.add(order1);
        orders.add(order2);
    }
    
    @Test
    @DisplayName("Test successful payment processing for single order")
    void testProcessOrderPayment_singleOrder_success() {
        // Given
        List<Order> singleOrder = List.of(orders.get(0));
        
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(singleOrder);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenReturn(paymentResponse);
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.of(createTestPayment()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<Order> processedOrders = orderService.processOrdersPayment(paymentInfo, accessToken);
        
        // Then
        assertNotNull(processedOrders);
        assertEquals(1, processedOrders.size());
        Order processedOrder = processedOrders.get(0);
        assertEquals(OrderStatus.PAYMENT_COMPLETED, processedOrder.getStatus());
        
        // Verify essential interactions without strict counts
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(PaymentInfo.class), any(BigDecimal.class));
        verify(productService).confirmStockReduction(any(Order.class));
        verify(orderRepository).save(any(Order.class));
    }
    
    @Test
    @DisplayName("Test successful payment processing for multiple orders")
    void testProcessOrderPayment_multipleOrders_success() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenReturn(paymentResponse);
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.of(createTestPayment()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<Order> processedOrders = orderService.processOrdersPayment(paymentInfo, accessToken);
        
        // Then
        assertNotNull(processedOrders);
        assertEquals(2, processedOrders.size());
        
        for (Order processedOrder : processedOrders) {
            assertEquals(OrderStatus.PAYMENT_COMPLETED, processedOrder.getStatus());
        }
        
        // Verify essential interactions without strict counts
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(PaymentInfo.class), any(BigDecimal.class));
        verify(productService, times(2)).confirmStockReduction(any(Order.class));
        verify(orderRepository, times(2)).save(any(Order.class));
        // Verify at least once but don't check exact count
        verify(paymentRepository, atLeastOnce()).findById(anyLong());
        // For the second order, a new payment copy should be created
        verify(paymentRepository).save(any(Payment.class));
    }
    
    @Test
    @DisplayName("Test payment processing with no orders found")
    void testProcessOrderPayment_noOrdersFound_throwsException() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(new ArrayList<>());
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("No orders found for this access token", exception.getMessage());
        
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("Test payment processing with orders in wrong status")
    void testProcessOrderPayment_invalidOrderStatus_throwsException() {
        // Given
        orders.get(0).setStatus(OrderStatus.PAYMENT_COMPLETED);
        
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("Order 1 is not pending payment", exception.getMessage());
        
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("Test payment processing with zero total amount")
    void testProcessOrderPayment_zeroTotalAmount_throwsException() {
        // Given
        orders.forEach(order -> order.setTotalPrice(BigDecimal.ZERO));
        
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("Total order amount must be greater than 0", exception.getMessage());
        
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("Test payment processing with payment failure")
    void testProcessOrderPayment_paymentFailure_throwsException() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenThrow(new ApiException(ErrorType.PAYMENT_FAILED, "Payment processing failed"));
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("Payment processing failed", exception.getMessage());
        
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(), any());
        
        // Orders should be marked as PAYMENT_FAILED
        verify(productService, times(2)).releaseStock(any(Order.class));
        verify(orderRepository, times(2)).save(argThat(order -> 
            order.getStatus() == OrderStatus.PAYMENT_FAILED
        ));
    }
    
    @Test
    @DisplayName("Test payment processing with missing payment record")
    void testProcessOrderPayment_missingPaymentRecord_throwsException() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenReturn(paymentResponse);
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("Payment not found", exception.getMessage());
        
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(), any());
        verify(paymentRepository).findById(anyLong());
    }
    
    @Test
    @DisplayName("Test payment processing with alternate payment method (Bitcoin)")
    void testProcessOrderPayment_alternatePaymentMethod_success() {
        // Given
        paymentInfo.setPaymentMethod(PaymentMethod.BITCOIN);
        paymentInfo.setTransactionHash("0x123456789abcdef");
        
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenReturn(paymentResponse);
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.of(createTestPayment()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<Order> processedOrders = orderService.processOrdersPayment(paymentInfo, accessToken);
        
        // Then
        assertNotNull(processedOrders);
        assertEquals(2, processedOrders.size());
        
        for (Order processedOrder : processedOrders) {
            assertEquals(OrderStatus.PAYMENT_COMPLETED, processedOrder.getStatus());
        }
        
        // Verify the correct payment method was used with relaxed verification
        verify(paymentService).processPayment(argThat(info -> 
            info.getPaymentMethod() == PaymentMethod.BITCOIN &&
            "0x123456789abcdef".equals(info.getTransactionHash())
        ), any());
    }
    
    @Test
    @DisplayName("Test payment processing with null access token")
    void testProcessOrderPayment_nullAccessToken_throwsException() {
        // Given
        String nullAccessToken = null;
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, nullAccessToken)
        );
        
        // Verify error message mentions access token
        assertTrue(exception.getMessage().contains("token") || 
                   exception.getMessage().contains("access") ||
                   exception.getMessage().contains("Access"));
        
        // Verify no payment was processed
        verify(paymentService, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("Test payment processing with expired access token")
    void testProcessOrderPayment_expiredAccessToken_throwsException() {
        // Given - Set up expired token
        String expiredAccessToken = "expired-token";
        
        // After analyzing OrderService.getOrdersByAccessToken method,
        // we see that it first calls tokenService.validateAccessToken(accessToken)
        // which returns email or null for invalid tokens
        
        // We need to make sure the token validation happens before accessing the repository
        // When validateAccessToken is called with expired token, return null
        when(tokenService.validateAccessToken(expiredAccessToken)).thenReturn(null);
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, expiredAccessToken)
        );
        
        // The exception should be INVALID_TOKEN, not TOKEN_EXPIRED 
        assertEquals("Invalid or expired access token", exception.getMessage());
        
        // Verify token validation was called but repository was never accessed
        verify(tokenService).validateAccessToken(expiredAccessToken);
        verify(orderRepository, never()).findAllByAccessToken(anyString());
        verify(paymentService, never()).processPayment(any(), any());
    }
    
    @Test
    @DisplayName("Test payment processing with invalid payment data")
    void testProcessOrderPayment_invalidPaymentData_throwsException() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        
        // Create invalid payment info (missing required fields)
        PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                .paymentMethod(PaymentMethod.CARD)
                // Missing card details
                .build();
                
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenThrow(new ApiException(ErrorType.VALIDATION_FAILED, "Invalid payment information"));
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(invalidPaymentInfo, accessToken)
        );
        
        assertEquals("Invalid payment information", exception.getMessage());
        
        // Verify payment was attempted but failed validation
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(), any());
        
        // Verify orders were updated to PAYMENT_FAILED
        verify(productService, times(2)).releaseStock(any(Order.class));
    }
    
    @Test
    @DisplayName("Test payment processing with payment gateway timeout")
    void testProcessOrderPayment_paymentGatewayTimeout_throwsException() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenThrow(new ApiException(ErrorType.SERVICE_UNAVAILABLE, "Payment gateway timeout"));
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("Payment gateway timeout", exception.getMessage());
        
        // Verify payment was attempted
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(), any());
        
        // Orders should be marked as PAYMENT_FAILED
        verify(productService, times(2)).releaseStock(any(Order.class));
        verify(orderRepository, times(2)).save(argThat(order -> 
            order.getStatus() == OrderStatus.PAYMENT_FAILED
        ));
    }
    
    @Test
    @DisplayName("Test payment processing where stock reservation fails")
    void testProcessOrderPayment_stockReservationFailure_throwsException() {
        // Given
        when(orderRepository.findAllByAccessToken(accessToken)).thenReturn(orders);
        when(paymentService.processPayment(any(PaymentInfo.class), any(BigDecimal.class)))
                .thenReturn(paymentResponse);
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.of(createTestPayment()));
        
        // Mock the stock confirmation to fail
        doThrow(new ApiException(ErrorType.INSUFFICIENT_STOCK, "Stock no longer available"))
            .when(productService).confirmStockReduction(any(Order.class));
        
        // When/Then
        ApiException exception = assertThrows(ApiException.class, () -> 
            orderService.processOrdersPayment(paymentInfo, accessToken)
        );
        
        assertEquals("Stock no longer available", exception.getMessage());
        
        // Verify payment was processed but stock confirmation failed
        verify(orderRepository).findAllByAccessToken(accessToken);
        verify(paymentService).processPayment(any(), any());
        verify(productService).confirmStockReduction(any(Order.class));
    }
    
    private Payment createTestPayment() {
        Payment payment = new Payment();
        payment.setPaymentId(1L);
        payment.setAmount(new BigDecimal("60.00"));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setTransactionId("txn_12345");
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }
} 