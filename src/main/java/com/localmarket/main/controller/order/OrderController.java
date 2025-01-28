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
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;
import com.localmarket.main.security.ProducerOnly;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {
    private final OrderService orderService;
    private final JwtService jwtService;

    

    @Operation(summary = "Create pending order", description = "Creates a pending order awaiting payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending order created", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/checkout")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
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
    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "accessToken", required = false) String accessToken) {
        if (token != null && token.startsWith("Bearer ")) {
            Long userId = jwtService.extractUserId(token.substring(7));
            return ResponseEntity.ok(orderService.getUserOrders(userId));
        } else if (accessToken != null) {
            return ResponseEntity.ok(orderService.getGuestOrders(accessToken));
        }
        throw new ApiException(ErrorType.ACCESS_DENIED, "Authentication required");
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

    @Operation(
        summary = "Update order status", 
        description = "Update order status (Producer only). Valid status transitions:\n\n" +
            "- PENDING → ACCEPTED or DECLINED\n" +
            "- ACCEPTED → DELIVERED or CANCELLED\n" +
            "- DELIVERED → RETURNED\n" +
            "- DECLINED/CANCELLED/RETURNED → No further transitions allowed"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order status updated successfully", 
            content = @Content(schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this order", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status transition", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{orderId}/status")
    @ProducerOnly
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @Parameter(description = "New status. Must follow valid transition rules", 
                schema = @Schema(allowableValues = {
                    "ACCEPTED", "DECLINED", "DELIVERED", "CANCELLED", "RETURNED"
                }))
            @RequestParam OrderStatus status,
            @RequestHeader("Authorization") String token) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, producerId));
    }

    @Operation(summary = "Get producer orders", description = "Get all orders containing producer's products")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders found", content = @Content(schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/producer-orders")
    @ProducerOnly
    public ResponseEntity<List<Order>> getProducerOrders(
            @RequestHeader("Authorization") String token) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.getProducerOrders(producerId));
    }

    @Operation(summary = "Get producer orders by status", description = "Get all orders containing producer's products filtered by status")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders found", content = @Content(schema = @Schema(implementation = Order.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/producer-orders/status/{status}")
    @ProducerOnly
    public ResponseEntity<List<Order>> getProducerOrdersByStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable OrderStatus status) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.getProducerOrdersByStatus(producerId, status));
    }
} 