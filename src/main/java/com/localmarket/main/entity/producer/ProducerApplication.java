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
    @Column(name = "applicationId")
    private Long applicationId;
    

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    private User customer;
    

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 255, message = "Business name must be between 2 and 255 characters")
    @Column(name = "businessName")
    private String businessName;
    

    @NotBlank(message = "Business description is required")
    @Size(max = 500, message = "Business description cannot exceed 500 characters")
    @Column(name = "businessDescription")
    private String businessDescription;
    

    @Column(name = "category_ids")
    private String categoryIds;
    
    @Size(max = 255, message = "Custom category cannot exceed 255 characters")
    @Column(name = "customCategory")
    private String customCategory;
    
    @NotBlank(message = "Business address is required")
    @Size(max = 500, message = "Business address cannot exceed 500 characters")
    @Column(name = "businessAddress")
    private String businessAddress;
    
    @NotBlank(message = "City/Region is required")
    @Column(name = "cityRegion")
    private String cityRegion;
    
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Column(name = "yearsOfExperience")
    private Integer yearsOfExperience;
    
    @Column(name = "websiteOrSocialLink")
    private String websiteOrSocialLink;

    @NotBlank(message = "Business phone number is required")
    @Column(name = "businessPhoneNumber")
    private String businessPhoneNumber;
    
    @Column(name = "messageToAdmin")
    private String messageToAdmin;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ApplicationStatus status = ApplicationStatus.PENDING;
    

    @Column(name = "declineReason")
    private String declineReason;
    
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @Column(name = "updatedAt")
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