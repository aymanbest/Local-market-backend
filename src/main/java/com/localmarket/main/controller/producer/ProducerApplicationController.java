package com.localmarket.main.controller.producer;

import com.localmarket.main.dto.producer.ProducerApplicationRequest;
import com.localmarket.main.dto.producer.ProducerApplicationDTO;
import com.localmarket.main.dto.producer.ApplicationDeclineRequest;

import com.localmarket.main.service.producer.ProducerApplicationService;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.localmarket.main.entity.producer.ApplicationStatus;

import java.util.List;

@RestController
@RequestMapping("/api/producer-applications")
@RequiredArgsConstructor
public class ProducerApplicationController {
    private final ProducerApplicationService applicationService;
    private final JwtService jwtService;

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ProducerApplicationDTO> submitApplication(
            @RequestBody ProducerApplicationRequest request,
            @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(applicationService.submitApplication(request, customerId));
    }

    @GetMapping
    @AdminOnly
    public ResponseEntity<List<ProducerApplicationDTO>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/pending")
    @AdminOnly
    public ResponseEntity<List<ProducerApplicationDTO>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(ApplicationStatus.PENDING));
    }

    @PostMapping("/{applicationId}/approve")
    @AdminOnly
    public ResponseEntity<ProducerApplicationDTO> approveApplication(
            @PathVariable Long applicationId,
            @RequestParam(required = false) Boolean approveCC) {
        return ResponseEntity.ok(applicationService.processApplication(applicationId, true, null, approveCC));
    }

    @PostMapping("/{applicationId}/decline")
    @AdminOnly
    public ResponseEntity<ProducerApplicationDTO> declineApplication(
            @PathVariable Long applicationId,
            @RequestBody ApplicationDeclineRequest request) {
        return ResponseEntity.ok(applicationService.processApplication(applicationId, false, request.getReason(), false));
    }

    @GetMapping("/my-application")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ProducerApplicationDTO> getMyApplication(
            @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(applicationService.getCustomerApplication(customerId));
    }

    @GetMapping("/status")
    public ResponseEntity<String> checkApplicationStatus(
            @RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(applicationService.checkApplicationStatus(customerId));
    }
} 