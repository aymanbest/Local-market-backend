package com.localmarket.main.entity.producer;

import com.localmarket.main.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ProducerApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    private User customer;
    
    private String businessName;
    private String businessDescription;
    private String categories;
    private String customCategory;
    private String businessAddress;
    private String cityRegion;
    private String customCityRegion;
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