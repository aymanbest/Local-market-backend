package com.localmarket.main.controller.order;

import com.localmarket.main.dto.order.OrderRequest;
import com.localmarket.main.entity.order.Order;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.localmarket.main.service.pdf.PdfGeneratorService;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.localmarket.main.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {
    private final OrderService orderService;
    private final PdfGeneratorService pdfGeneratorService;
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Operation(summary = "Create pending order", description = "Creates a pending order awaiting payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending order created", content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Creating order. User authenticated: {}, User details: {}", 
            userDetails != null, 
            userDetails != null ? userDetails.getEmail() : "null");
            
        String userEmail = userDetails != null ? userDetails.getEmail() : null;
        
        // If user is authenticated, we don't need guest email
        if (userDetails != null) {
            request.setGuestEmail(null);
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
    public ResponseEntity<Page<Order>> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "accessToken", required = false) String accessToken,
            @RequestParam(value = "showall", required = false) String showall,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        if (accessToken != null) {
            List<Order> orders = orderService.getOrdersByAccessToken(accessToken);
            return ResponseEntity.ok(new PageImpl<>(orders, pageable, orders.size()));
        }

        if (userDetails != null && "true".equals(showall)) {
            return ResponseEntity.ok(orderService.getUserOrdersPaginated(userDetails.getId(), pageable));
        }

        throw new ApiException(ErrorType.ACCESS_DENIED, "Authentication required");
    }

    @Operation(summary = "Get my orders by status", description = "Retrieve all orders for authenticated user by status")
    @GetMapping("/my-orders/status/{status}")
    public ResponseEntity<List<Order>> getMyOrdersByStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getUserOrdersByStatus(userDetails.getId(), status));
    }

    @Operation(summary = "Get my order by ID", description = "Retrieve specific order for authenticated user")
    @GetMapping("/my-orders/{orderId}")
    public ResponseEntity<Order> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getUserOrder(orderId, userDetails.getId()));
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
            @Parameter(description = "New status. Must follow valid transition rules", 
                schema = @Schema(allowableValues = {
                    "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"
            })) @RequestParam OrderStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status, userDetails.getId()));
    }

    @Operation(summary = "Get producer orders", description = "Get all orders containing producer's products")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders found", content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/producer-orders")
    @ProducerOnly
    public ResponseEntity<Page<Order>> getProducerOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String customerEmail) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(orderService.getProducerOrders(userDetails.getId(), customerEmail, pageable));
    }

    @Operation(summary = "Get producer orders by status", description = "Get all orders containing producer's products filtered by status")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders found", content = @Content(schema = @Schema(implementation = Order.class))),
            @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/producer-orders/status/{status}")
    @ProducerOnly
    public ResponseEntity<Page<Order>> getProducerOrdersByStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String customerEmail) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(orderService.getProducerOrdersByStatus(userDetails.getId(), status, customerEmail, pageable));
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        List<Order> orders;
        if (accessToken != null) {
            orders = orderService.getOrdersByAccessToken(accessToken);
        } else {
            if (userDetails == null) {
                throw new ApiException(ErrorType.ACCESS_DENIED, "Authentication required");
            }
            orders = orderService.getUserOrders(userDetails.getId());
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