package com.localmarket.main.entity.producer;

import com.localmarket.main.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;


@Entity
@Data
public class ProducerApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;
    
    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    private User customer;
    
    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 255, message = "Business name must be between 2 and 255 characters")
    private String businessName;
    
    @NotBlank(message = "Business description is required")
    @Size(max = 500, message = "Business description cannot exceed 500 characters")
    private String businessDescription;
    
    @NotBlank(message = "Categories are required")
    private String categories;
    
    @Size(max = 255, message = "Custom category cannot exceed 255 characters")
    private String customCategory;
    
    @NotBlank(message = "Business address is required")
    @Size(max = 500, message = "Business address cannot exceed 500 characters")
    private String businessAddress;
    
    @NotBlank(message = "City/Region is required")
    private String cityRegion;
    
    @Min(value = 0, message = "Years of experience cannot be negative")
    private Integer yearsOfExperience;
    
    private String websiteOrSocialLink;
    
    private String messageToAdmin;
    
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;
    
    private String declineReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}