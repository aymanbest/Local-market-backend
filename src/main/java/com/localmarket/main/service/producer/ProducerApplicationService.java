package com.localmarket.main.service.producer;

import com.localmarket.main.entity.producer.ProducerApplication;
import com.localmarket.main.repository.producer.ProducerApplicationRepository;
import com.localmarket.main.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.dto.producer.ProducerApplicationRequest;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.entity.producer.ApplicationStatus;
import java.util.List;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.dto.producer.ProducerApplicationDTO;
import java.util.Optional;
import com.localmarket.main.util.InputValidator;
import com.localmarket.main.dto.category.CategoryRequest;
import com.localmarket.main.service.category.CategoryService;


@Service
@RequiredArgsConstructor
public class ProducerApplicationService {
    private final ProducerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;



    @Transactional
    public ProducerApplicationDTO submitApplication(ProducerApplicationRequest request, Long customerId) {
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Customer not found"));
            
        if (customer.getRole() != Role.CUSTOMER) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Only customers can apply to become producers");
        }
        
        if (applicationRepository.findByCustomerAndStatus(customer, ApplicationStatus.PENDING).isPresent()) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "You already have a pending application");
        }

        // Validate custom category if present
        if (request.getCustomCategory() != null && !request.getCustomCategory().trim().isEmpty()) {
            request.setCustomCategory(InputValidator.sanitizeCategory(request.getCustomCategory()));
        }

        ProducerApplication application = new ProducerApplication();
        application.setCustomer(customer);
        application.setBusinessName(request.getBusinessName());
        application.setBusinessDescription(request.getBusinessDescription());
        application.setCategories(String.join(",", request.getCategories()));
        application.setCustomCategory(request.getCustomCategory());
        application.setBusinessAddress(request.getBusinessAddress());
        application.setCityRegion(request.getCityRegion());
        application.setYearsOfExperience(request.getYearsOfExperience());
        application.setWebsiteOrSocialLink(request.getWebsiteOrSocialLink());
        application.setMessageToAdmin(request.getMessageToAdmin());
        
        return mapToDTO(applicationRepository.save(application));
    }

    private ProducerApplicationDTO mapToDTO(ProducerApplication application) {
        return ProducerApplicationDTO.builder()
                .applicationId(application.getApplicationId())
                .customerEmail(application.getCustomer().getEmail())
                .customerUsername(application.getCustomer().getUsername())
                .businessName(application.getBusinessName())
                .businessDescription(application.getBusinessDescription())
                .categories(application.getCategories().split(","))
                .customCategory(application.getCustomCategory())
                .businessAddress(application.getBusinessAddress())
                .cityRegion(application.getCityRegion())
                .yearsOfExperience(application.getYearsOfExperience())
                .websiteOrSocialLink(application.getWebsiteOrSocialLink())
                .messageToAdmin(application.getMessageToAdmin())
                .status(application.getStatus())
                .declineReason(application.getDeclineReason())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    @Transactional
    public ProducerApplicationDTO processApplication(Long applicationId, boolean approved, String declineReason) {
        ProducerApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Application not found"));
            
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Application has already been processed");
        }
        
        if (approved) {
            application.setStatus(ApplicationStatus.APPROVED);
            User customer = application.getCustomer();
            customer.setRole(Role.PRODUCER);
            
            // Increment token version and reset to 0 if it reaches 10
            int newVersion = (customer.getTokenVersion() + 1) % 10;
            customer.setTokenVersion(newVersion);
            userRepository.save(customer);
            
            // Handle custom category if present
            if (application.getCustomCategory() != null && !application.getCustomCategory().trim().isEmpty()) {
                String customCategory = application.getCustomCategory().trim();
                CategoryRequest categoryRequest = new CategoryRequest();
                categoryRequest.setName(customCategory);
                
                try {
                    // Find an admin user to create the category
                    User admin = userRepository.findByRole(Role.ADMIN)
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "No admin found to create category"));
                    
                    categoryService.createCategory(categoryRequest, admin.getUserId());
                    
                    // Update application categories
                    String categories = application.getCategories();
                    application.setCategories(categories + "," + customCategory);
                } catch (ApiException e) {
                    if (e.getErrorType() == ErrorType.DUPLICATE_RESOURCE) {
                        String categories = application.getCategories();
                        application.setCategories(categories + "," + customCategory);
                    } else {
                        throw e;
                    }
                }
            }
        } else {
            application.setStatus(ApplicationStatus.DECLINED);
            application.setDeclineReason(declineReason);
        }
        
        return mapToDTO(applicationRepository.save(application));
    }

    public List<ProducerApplicationDTO> getAllApplications() {
        return applicationRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<ProducerApplicationDTO> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .toList();
    }

    public ProducerApplicationDTO getCustomerApplication(Long customerId) {
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Customer not found"));
        ProducerApplication application = applicationRepository.findByCustomer(customer)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "No application found"));
        return mapToDTO(application);
    }

    public String checkApplicationStatus(Long customerId) {
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Customer not found"));
            
        Optional<ProducerApplication> application = applicationRepository.findByCustomer(customer);
        
        if (application.isEmpty()) {
            return "NO_APPLICATION";
        }
        
        return application.get().getStatus().name();
    }
}
