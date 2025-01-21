package com.localmarket.main.dto.producer;

import lombok.Data;

@Data
public class ProducerApplicationRequest {
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
}
