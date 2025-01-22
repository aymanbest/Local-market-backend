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
import com.localmarket.main.dto.category.CategoryRequest;
import com.localmarket.main.service.category.CategoryService;
import java.util.Set;
import java.util.stream.Collectors;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.entity.category.Category;
import java.util.Arrays;
import com.localmarket.main.dto.category.CategoryDTO;


@Service
@RequiredArgsConstructor
public class ProducerApplicationService {
    private final ProducerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;



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

        // Validate that all category IDs exist
        Set<Long> validCategoryIds = categoryRepository.findAllById(request.getCategoryIds())
            .stream()
            .map(Category::getCategoryId)
            .collect(Collectors.toSet());
        
        if (validCategoryIds.size() != request.getCategoryIds().size()) {
            throw new ApiException(ErrorType.RESOURCE_NOT_FOUND, "One or more categories not found");
        }

        ProducerApplication application = new ProducerApplication();
        application.setCustomer(customer);
        application.setBusinessName(request.getBusinessName());
        application.setBusinessDescription(request.getBusinessDescription());
        application.setCategoryIds(request.getCategoryIds().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        application.setCustomCategory(request.getCustomCategory());
        application.setBusinessAddress(request.getBusinessAddress());
        application.setCityRegion(request.getCityRegion());
        application.setYearsOfExperience(request.getYearsOfExperience());
        application.setWebsiteOrSocialLink(request.getWebsiteOrSocialLink());
        application.setMessageToAdmin(request.getMessageToAdmin());
        
        return mapToDTO(applicationRepository.save(application));
    }

    private ProducerApplicationDTO mapToDTO(ProducerApplication application) {
        String[] categories = Arrays.stream(application.getCategoryIds().split(","))
            .map(id -> categoryRepository.getReferenceById(Long.parseLong(id)).getName())
            .toArray(String[]::new);

        return ProducerApplicationDTO.builder()
                .applicationId(application.getApplicationId())
                .customerEmail(application.getCustomer().getEmail())
                .customerUsername(application.getCustomer().getUsername())
                .businessName(application.getBusinessName())
                .businessDescription(application.getBusinessDescription())
                .categories(categories)
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
    public ProducerApplicationDTO processApplication(Long applicationId, boolean approved, String declineReason, Boolean approveCustomCategory) {
        ProducerApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Application not found"));
            
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Application has already been processed");
        }
        
        if (approved) {
            application.setStatus(ApplicationStatus.APPROVED);
            User customer = application.getCustomer();
            customer.setRole(Role.PRODUCER);
            
            int newVersion = (customer.getTokenVersion() + 1) % 10;
            customer.setTokenVersion(newVersion);
            userRepository.save(customer);
            
            // Handle custom category if present and admin approved it
            if (approveCustomCategory != null && approveCustomCategory 
                && application.getCustomCategory() != null 
                && !application.getCustomCategory().trim().isEmpty()) {
                
                String customCategory = application.getCustomCategory().trim();
                CategoryRequest categoryRequest = new CategoryRequest();
                categoryRequest.setName(customCategory);
                
                try {
                    CategoryDTO newCategory = categoryService.createCategory(categoryRequest);
                    String categories = application.getCategoryIds();
                    application.setCategoryIds(categories + "," + newCategory.getCategoryId());
                } catch (ApiException e) {
                    if (e.getErrorType() == ErrorType.CATEGORY_ALREADY_EXISTS) {
                        Category existingCategory = categoryRepository.findByName(customCategory)
                            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Category not found"));
                        String categories = application.getCategoryIds();
                        application.setCategoryIds(categories + "," + existingCategory.getCategoryId());
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
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "No application found for this customer"));
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
