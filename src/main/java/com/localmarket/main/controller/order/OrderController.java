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
import com.localmarket.main.dto.user.UserInfo;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {
    private final OrderService orderService;
    private final JwtService jwtService;

    @Operation(summary = "Create a new order", description = "Creates an order for authenticated or guest users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order created successfully", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        UserInfo userInfo = null;
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            userInfo = new UserInfo(
                jwtService.extractUsername(jwt),
                jwtService.extractUserId(jwt),
                jwtService.extractRole(jwt)
            );
        }
        return ResponseEntity.ok(orderService.createOrder(request, userInfo));
    }

    @Operation(summary = "Create pending order", description = "Creates a pending order awaiting payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending order created", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Process payment", description = "Process payment for an existing order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment processed successfully", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid payment information", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Get user orders", description = "Retrieve all orders for authenticated user")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(
            @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @Operation(summary = "Get order by ID", description = "Get specific order details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order found", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}")
    // @PreAuthorize("permitAll()")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "guestToken", required = false) String guestToken) {
        if (token == null && guestToken == null) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Authentication required");
        }
        String userEmail = null;
        if (token != null && token.startsWith("Bearer ")) {
            userEmail = jwtService.extractUsername(token.substring(7));
        }
        return ResponseEntity.ok(orderService.getOrder(orderId, userEmail, guestToken));
    }

    @Operation(summary = "Get my orders", description = "Retrieve all orders for authenticated user")
    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @Operation(summary = "Get my orders by status", description = "Retrieve all orders for authenticated user by status")
    @GetMapping("/my-orders/status/{status}")
    public ResponseEntity<List<Order>> getMyOrdersByStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable OrderStatus status) {
        String jwt = token.substring(7);
        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userId, status));
    }

    @Operation(summary = "Get my order by ID", description = "Retrieve specific order for authenticated user")
    @GetMapping("/my-orders/{orderId}")
    public ResponseEntity<Order> getMyOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable Long orderId) {
        String jwt = token.substring(7);
        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getUserOrder(orderId, userId));
    }
} 