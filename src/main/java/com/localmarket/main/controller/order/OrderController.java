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
import jakarta.servlet.http.HttpServletRequest;
import com.localmarket.main.util.CookieUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.localmarket.main.service.pdf.PdfGeneratorService;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {
    private final OrderService orderService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final PdfGeneratorService pdfGeneratorService;

    @Operation(summary = "Create pending order", description = "Creates a pending order awaiting payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending order created", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            HttpServletRequest requestco) {
        String userEmail = null;
        try {
            String jwt = cookieUtil.getJwtFromRequest(requestco);
            if (jwt != null) {
                userEmail = jwtService.extractUsername(jwt);
            }
        } catch (Exception e) {
            // Continue as guest if no valid token
        }
        return ResponseEntity.ok(orderService.createPendingOrder(request, userEmail));
    }

    @Operation(summary = "Process payment", description = "Process payment for an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payment information", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/pay")
    public ResponseEntity<List<Order>> processPayment(
            @RequestBody PaymentInfo paymentInfo,
            @RequestParam(value = "accessToken", required = false) String accessToken) {
        return ResponseEntity.ok(orderService.processOrdersPayment(paymentInfo, accessToken));
    }

    @Operation(summary = "Get user orders", description = "Retrieve order details using access token or authentication")
    @SecurityRequirement(name = "cookie")
    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            HttpServletRequest request,
            @RequestParam(value = "accessToken", required = false) String accessToken,
            @RequestParam(value = "showall", required = false) String showall) {

        if (accessToken != null) {
            return ResponseEntity.ok(orderService.getOrdersByAccessToken(accessToken));
        }

        String jwt = cookieUtil.getJwtFromRequest(request);
        if (jwt != null && "true".equals(showall)) {
            Long userId = jwtService.extractUserId(jwt);
            return ResponseEntity.ok(orderService.getUserOrders(userId));
        }

        throw new ApiException(ErrorType.ACCESS_DENIED, "Authentication required");
    }

    @Operation(summary = "Get my orders by status", description = "Retrieve all orders for authenticated user by status")
    @GetMapping("/my-orders/status/{status}")
    public ResponseEntity<List<Order>> getMyOrdersByStatus(
            HttpServletRequest request,
            @PathVariable OrderStatus status) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userId, status));
    }

    @Operation(summary = "Get my order by ID", description = "Retrieve specific order for authenticated user")
    @GetMapping("/my-orders/{orderId}")
    public ResponseEntity<Order> getMyOrder(
            HttpServletRequest request,
            @PathVariable Long orderId) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        Long userId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getUserOrder(orderId, userId));
    }

    @Operation(summary = "Update order status", description = "Update order status (Producer only). Valid status transitions:\n\n"
            +
            "- PENDING_PAYMENT → No transitions allowed (automatic)\n" +
            "- PAYMENT_FAILED → No transitions allowed (automatic)\n" +
            "- PAYMENT_COMPLETED → PROCESSING\n" +
            "- PROCESSING → SHIPPED or CANCELLED\n" +
            "- SHIPPED → DELIVERED\n" +
            "- DELIVERED → RETURNED\n" +
            "- CANCELLED/RETURNED → No further transitions allowed")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully", content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this order", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{orderId}/status")
    @ProducerOnly
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @Parameter(description = "New status. Must follow valid transition rules", schema = @Schema(allowableValues = {
                    "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"
            })) @RequestParam OrderStatus status,
            HttpServletRequest request) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        Long producerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, producerId));
    }

    @Operation(summary = "Get producer orders", description = "Get all orders containing producer's products")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders found", content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/producer-orders")
    @ProducerOnly
    public ResponseEntity<List<Order>> getProducerOrders(
            HttpServletRequest request) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        Long producerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getProducerOrders(producerId));
    }

    @Operation(summary = "Get producer orders by status", description = "Get all orders containing producer's products filtered by status")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders found", content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/producer-orders/status/{status}")
    @ProducerOnly
    public ResponseEntity<List<Order>> getProducerOrdersByStatus(
            HttpServletRequest request,
            @PathVariable OrderStatus status) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        Long producerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(orderService.getProducerOrdersByStatus(producerId, status));
    }

    @Operation(summary = "Get orders by access token", description = "Retrieve all orders associated with an access token")
    @GetMapping("/bundle/{accessToken}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<Order>> getOrderBundle(
            @PathVariable String accessToken) {
        return ResponseEntity.ok(orderService.getOrderBundle(accessToken));
    }

    @Operation(summary = "Get order receipt PDF", description = "Generate PDF receipt for an order")
    @GetMapping("/receipt")
    public ResponseEntity<Resource> getOrderReceipt(
            @RequestParam(value = "accessToken", required = false) String accessToken,
            HttpServletRequest request) {
        
        List<Order> orders;
        if (accessToken != null) {
            orders = orderService.getOrdersByAccessToken(accessToken);
        } else {
            String jwt = cookieUtil.getJwtFromRequest(request);
            if (jwt == null) {
                throw new ApiException(ErrorType.ACCESS_DENIED, "Authentication required");
            }
            Long userId = jwtService.extractUserId(jwt);
            orders = orderService.getUserOrders(userId);
        }

        if (orders.isEmpty()) {
            throw new ApiException(ErrorType.NOT_FOUND, "No orders found");
        }

        byte[] pdfContent = pdfGeneratorService.generateReceipt(orders.get(0));
        ByteArrayResource resource = new ByteArrayResource(pdfContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=receipt_" + orders.get(0).getOrderId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}