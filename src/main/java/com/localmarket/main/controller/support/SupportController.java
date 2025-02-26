package com.localmarket.main.controller.support;

import com.localmarket.main.dto.support.*;
import com.localmarket.main.entity.support.TicketMessage;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.service.support.SupportService;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.service.user.UserService;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import com.localmarket.main.entity.support.TicketStatus;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Support ticket management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class SupportController {
    private final SupportService supportService;
    private final UserService userService;

    @PostMapping("/tickets")
    @ProducerOnly
    @Operation(
        summary = "Create a new support ticket", 
        description = "Allows producers to create a new support ticket. Only one active ticket per producer is allowed."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket created successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to create tickets"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Producer already has an active ticket")
    })
    public ResponseEntity<?> createTicket(
            @RequestBody CreateTicketRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                log.error("No authenticated user found");
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("User not authenticated"));
            }

            // Get the actual user from UserService using the username
            User user = userService.getUserByUsername(userDetails.getUsername());
            if (user == null) {
                log.error("User not found in database: {}", userDetails.getUsername());
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("User not found"));
            }

            if (user.getRole() != Role.PRODUCER) {
                log.error("User {} attempted to create ticket but is not a producer", user.getUsername());
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only producers can create tickets"));
            }

            log.info("Producer {} attempting to create ticket", user.getUsername());
            
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Subject is required"));
            }

            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Message is required"));
            }

            TicketResponse response = supportService.createTicket(request, user);
            log.info("Ticket created successfully for producer {}", user.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already have an active ticket")) {
                log.warn("Producer {} attempted to create multiple tickets", userDetails.getUsername());
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
            }
            log.error("Error creating ticket for producer {}: {}", userDetails.getUsername(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/tickets/producer")
    @ProducerOnly
    @Operation(summary = "Get producer's tickets", description = "Retrieves all tickets created by the authenticated producer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of tickets retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view tickets")
    })
    public ResponseEntity<Page<TicketResponse>> getProducerTickets(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(supportService.getProducerTickets(user, status, pageable));
    }

    @GetMapping("/tickets/admin")
    @AdminOnly
    @Operation(summary = "Get admin's assigned tickets", description = "Retrieves all tickets assigned to the authenticated admin")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of tickets retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view admin tickets")
    })
    public ResponseEntity<Page<TicketResponse>> getAdminTickets(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(supportService.getAdminTickets(user, status, pageable));
    }

    @GetMapping("/tickets/unassigned")
    @AdminOnly
    @Operation(summary = "Get unassigned tickets", description = "Retrieves all tickets that haven't been assigned to any admin")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of unassigned tickets retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view unassigned tickets")
    })
    public ResponseEntity<Page<TicketResponse>> getUnassignedTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(supportService.getUnassignedTickets(pageable));
    }

    @PostMapping("/tickets/{ticketId}/assign")
    @AdminOnly
    @Operation(summary = "Assign ticket to self", description = "Allows an admin to assign a ticket to themselves")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket assigned successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to assign tickets"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<TicketResponse> assignTicket(
            @Parameter(description = "ID of the ticket to assign") @PathVariable Long ticketId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(supportService.assignTicket(ticketId, user));
    }

    @PostMapping("/tickets/{ticketId}/forward")
    @AdminOnly
    @Operation(summary = "Forward ticket to another admin", description = "Allows an admin to forward a ticket to another admin")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket forwarded successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to forward tickets"),
        @ApiResponse(responseCode = "404", description = "Ticket or admin not found"),
        @ApiResponse(responseCode = "400", description = "Target user is not an admin")
    })
    public ResponseEntity<TicketResponse> forwardTicket(
            @Parameter(description = "ID of the ticket to forward") @PathVariable Long ticketId,
            @Parameter(description = "ID of the admin to forward the ticket to") @RequestParam Long newAdminId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        User newAdmin = userService.getUserById(newAdminId);
        if (newAdmin == null) {
            throw new RuntimeException("Target admin not found");
        }
        if (newAdmin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Target user is not an admin");
        }
        return ResponseEntity.ok(supportService.forwardTicket(ticketId, newAdmin));
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PRODUCER')")
    @Operation(summary = "Add message to ticket", description = "Allows admins and producers to add messages to a ticket")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message added successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to add messages"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Void> addMessage(
            @Parameter(description = "ID of the ticket to add message to") @PathVariable Long ticketId,
            @RequestBody TicketMessageRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        supportService.addMessage(ticketId, request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tickets/{ticketId}/messages")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PRODUCER')")
    @Operation(summary = "Get ticket messages", description = "Retrieves all messages for a specific ticket. Internal notes are only visible to admins.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to view messages"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<List<TicketMessageResponse>> getTicketMessages(
            @Parameter(description = "ID of the ticket to get messages from") @PathVariable Long ticketId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(supportService.getTicketMessages(ticketId, user));
    }

    @PostMapping("/tickets/{ticketId}/close")
    @AdminOnly
    @Operation(summary = "Close ticket", description = "Allows an admin to close a ticket")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ticket closed successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to close tickets"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<TicketResponse> closeTicket(
            @Parameter(description = "ID of the ticket to close") @PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.closeTicket(ticketId));
    }
}

@Data
class ErrorResponse {
    private final String message;
    private final long timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
} 