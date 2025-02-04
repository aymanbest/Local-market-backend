package com.localmarket.main.dto.producer;

import lombok.Data;
import java.util.Set;

@Data
public class ProducerApplicationRequest {
    private String businessName;
    private String businessDescription;
    private Set<Long> categoryIds;
    private String customCategory;
    private String businessAddress;
    private String businessPhoneNumber;
    private String cityRegion;
    private String customCityRegion;
    private Integer yearsOfExperience;
    private String websiteOrSocialLink;
    private String messageToAdmin;
}
