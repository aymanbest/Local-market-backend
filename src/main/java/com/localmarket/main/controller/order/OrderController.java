package com.localmarket.main.controller.order;

import com.localmarket.main.dto.order.OrderRequest;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
} 