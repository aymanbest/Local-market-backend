package com.localmarket.main.controller.producer;

import com.localmarket.main.dto.producer.ProducerApplicationRequest;
import com.localmarket.main.dto.producer.ProducerApplicationResponse;
import com.localmarket.main.dto.producer.ApplicationDeclineRequest;

import com.localmarket.main.service.producer.ProducerApplicationService;
import com.localmarket.main.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.localmarket.main.security.CustomUserDetails;
import com.localmarket.main.entity.producer.ApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.localmarket.main.dto.producer.ApplicationStatusResponse;

@RestController
@RequestMapping("/api/producer-applications")
@RequiredArgsConstructor
@Tag(name = "Producer Applications", description = "Producer application management APIs")

public class ProducerApplicationController {
    private final ProducerApplicationService applicationService;

    @Operation(summary = "Submit producer application", description = "Submit application to become a producer")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application submitted successfully", content = @Content(schema = @Schema(implementation = ProducerApplicationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as customer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ProducerApplicationResponse> submitApplication(
            @RequestBody ProducerApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.submitApplication(request, userDetails.getId()));
    }

    @Operation(summary = "Get all applications", description = "Get all producer applications (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully", content = @Content(schema = @Schema(implementation = ProducerApplicationResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @AdminOnly
    public ResponseEntity<Page<ProducerApplicationResponse>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(applicationService.getAllApplications(pageable));
    }

    @Operation(summary = "Get pending applications", description = "Get all pending producer applications (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending applications retrieved successfully", content = @Content(schema = @Schema(implementation = ProducerApplicationResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/pending")
    @AdminOnly
    public ResponseEntity<Page<ProducerApplicationResponse>> getPendingApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(ApplicationStatus.PENDING, pageable));
    }

    @Operation(summary = "Approve application", description = "Approve producer application (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application approved", content = @Content(schema = @Schema(implementation = ProducerApplicationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{applicationId}/approve")
    @AdminOnly
    public ResponseEntity<ProducerApplicationResponse> approveApplication(
            @PathVariable Long applicationId,
            @RequestParam(required = false) Boolean approveCC) {
        return ResponseEntity.ok(applicationService.processApplication(applicationId, true, null, approveCC));
    }
    
    @Operation(summary = "Decline application", description = "Decline producer application (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application declined", content = @Content(schema = @Schema(implementation = ProducerApplicationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{applicationId}/decline")
    @AdminOnly
    public ResponseEntity<ProducerApplicationResponse> declineApplication(
            @PathVariable Long applicationId,
            @RequestBody ApplicationDeclineRequest request) {
        return ResponseEntity.ok(applicationService.processApplication(applicationId, false, request.getReason(), false));
    }

    @Operation(summary = "Get my application", description = "Get my producer application")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application retrieved successfully", content = @Content(schema = @Schema(implementation = ProducerApplicationResponse.class))),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as customer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my-application")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ProducerApplicationResponse> getMyApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.getCustomerApplication(userDetails.getId()));
    }

    
    @Operation(summary = "Check application status", description = "Check my producer application status")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application status retrieved successfully", content = @Content(schema = @Schema(implementation = ApplicationStatusResponse.class))),
        @ApiResponse(responseCode = "404", description = "Application not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as customer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApplicationStatusResponse> checkApplicationStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(applicationService.checkApplicationStatus(userDetails.getId()));
    }

} 