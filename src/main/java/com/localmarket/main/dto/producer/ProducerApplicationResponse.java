package com.localmarket.main.dto.producer;

import com.localmarket.main.entity.producer.ApplicationStatus;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class ProducerApplicationResponse {
    private Long applicationId;
    private String customerEmail;
    private String customerUsername;
    private String businessName;
    private String businessDescription;
    private String[] categories;
    private String customCategory;
    private String businessAddress;
    private String cityRegion;
    private String customCityRegion;
    private Integer yearsOfExperience;
    private String websiteOrSocialLink;
    private String messageToAdmin;
    private ApplicationStatus status;
    private String declineReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 