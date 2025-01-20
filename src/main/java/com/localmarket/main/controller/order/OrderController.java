package com.localmarket.main.controller.order;

import com.localmarket.main.dto.order.OrderRequest;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.service.order.OrderService;
import com.localmarket.main.dto.payment.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.entity.order.OrderStatus;
import java.util.List;
import com.localmarket.main.dto.order.OrderResponse;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final JwtService jwtService;

    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userEmail = null;
        if (token != null && token.startsWith("Bearer ")) {
            userEmail = jwtService.extractUsername(token.substring(7));
        }
        return ResponseEntity.ok(orderService.createOrder(request, userEmail));
    }

    @PostMapping("/checkout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<OrderResponse> createPendingOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userEmail = null;
        if (token != null && token.startsWith("Bearer ")) {
            userEmail = jwtService.extractUsername(token.substring(7));
        }
        return ResponseEntity.ok(orderService.createPendingOrder(request, userEmail));
    }

    @PostMapping("/{orderId}/pay")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Order> processPayment(
            @PathVariable Long orderId,
            @RequestBody PaymentInfo paymentInfo,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userEmail = null;
        if (token != null && token.startsWith("Bearer ")) {
            userEmail = jwtService.extractUsername(token.substring(7));
        }
        return ResponseEntity.ok(orderService.processOrderPayment(orderId, paymentInfo, userEmail));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(
            @RequestHeader("Authorization") String token) {
        String userEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(orderService.getUserOrders(userEmail));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        String userEmail = null;
        if (token != null && token.startsWith("Bearer ")) {
            userEmail = jwtService.extractUsername(token.substring(7));
        }
        return ResponseEntity.ok(orderService.getOrder(orderId, userEmail));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("Authorization") String token) {
        String userEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(orderService.getUserOrders(userEmail));
    }

    @GetMapping("/my-orders/status/{status}")
    public ResponseEntity<List<Order>> getMyOrdersByStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable OrderStatus status) {
        String userEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userEmail, status));
    }

    @GetMapping("/my-orders/{orderId}")
    public ResponseEntity<Order> getMyOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long orderId) {
        String userEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(orderService.getUserOrder(orderId, userEmail));
    }
} 