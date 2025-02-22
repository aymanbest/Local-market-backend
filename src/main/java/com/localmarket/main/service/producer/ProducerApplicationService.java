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
import com.localmarket.main.dto.producer.ProducerApplicationResponse;
import java.util.Optional;
import com.localmarket.main.dto.category.CategoryRequest;
import com.localmarket.main.service.category.CategoryService;
import java.util.Set;
import java.util.stream.Collectors;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.entity.category.Category;
import java.util.Arrays;
import com.localmarket.main.dto.category.CategoryResponse;
import com.localmarket.main.service.notification.admin.AdminNotificationService;
import com.localmarket.main.service.email.EmailService;
import jakarta.mail.MessagingException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import com.localmarket.main.dto.producer.ApplicationStatusResponse;


@Service
@RequiredArgsConstructor
public class ProducerApplicationService {
    private final ProducerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final AdminNotificationService adminNotificationService;
    private final EmailService emailService;
    private static final Logger log = LoggerFactory.getLogger(ProducerApplicationService.class);




    @Transactional
    public ProducerApplicationResponse submitApplication(ProducerApplicationRequest request, Long customerId) {
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
        application.setBusinessPhoneNumber(request.getBusinessPhoneNumber());
        application.setCategoryIds(request.getCategoryIds().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        application.setCustomCategory(request.getCustomCategory());
        application.setBusinessAddress(request.getBusinessAddress());
        application.setCityRegion(request.getCityRegion());
        application.setYearsOfExperience(request.getYearsOfExperience());
        application.setWebsiteOrSocialLink(request.getWebsiteOrSocialLink());
        application.setMessageToAdmin(request.getMessageToAdmin());

        adminNotificationService.notifyNewProducerApplication(application);
        
        return mapToDTO(applicationRepository.save(application));
    }

    private ProducerApplicationResponse mapToDTO(ProducerApplication application) {
        String[] categories = application.getCategoryIds() == null || application.getCategoryIds().isEmpty() 
            ? new String[0]
            : Arrays.stream(application.getCategoryIds().split(","))
                .map(id -> categoryRepository.getReferenceById(Long.parseLong(id)).getName())
                .toArray(String[]::new);

        return ProducerApplicationResponse.builder()
                .applicationId(application.getApplicationId())
                .customerEmail(application.getCustomer().getEmail())
                .customerUsername(application.getCustomer().getUsername())
                .businessName(application.getBusinessName())
                .businessDescription(application.getBusinessDescription())
                .categories(categories)
                .customCategory(application.getCustomCategory())
                .businessAddress(application.getBusinessAddress())
                .businessPhoneNumber(application.getBusinessPhoneNumber())
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
    public ProducerApplicationResponse processApplication(Long applicationId, boolean approved, String declineReason, Boolean approveCustomCategory) {
        ProducerApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Application not found"));
            
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Application has already been processed");
        }
        
        if (approved) {
            application.setStatus(ApplicationStatus.APPROVED);
            
            try {
                emailService.sendHtmlEmail(
                    application.getCustomer().getEmail(),
                    "Producer Application Approved",
                    application.getCustomer().getUsername(),
                    "producer-accepted-email",
                    null,
                    new HashMap<>()
                );
            } catch (Exception e) {
                log.error("Failed to send producer accepted email to {}: {}", application.getCustomer().getEmail(), e.getMessage());
            }


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
                    CategoryResponse newCategory = categoryService.createCategory(categoryRequest);
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

    @Transactional(readOnly = true)
    public Page<ProducerApplicationResponse> getAllApplications(Pageable pageable) {
        List<ProducerApplication> applications = applicationRepository.findAll();
        
        // Sort applications
        List<ProducerApplication> sortedApplications = applications.stream()
            .sorted((a1, a2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "createdAt" -> a1.getCreatedAt().compareTo(a2.getCreatedAt());
                    case "status" -> a1.getStatus().compareTo(a2.getStatus());
                    case "businessName" -> a1.getBusinessName().compareTo(a2.getBusinessName());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedApplications.size());
        
        if (start >= sortedApplications.size()) {
            return new PageImpl<>(List.of(), pageable, sortedApplications.size());
        }
        
        List<ProducerApplication> paginatedApplications = sortedApplications.subList(start, end);
        
        return new PageImpl<>(
            paginatedApplications.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()),
            pageable,
            sortedApplications.size()
        );
    }

    @Transactional(readOnly = true)
    public Page<ProducerApplicationResponse> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        List<ProducerApplication> applications = applicationRepository.findByStatus(status);
        
        // Sort applications
        List<ProducerApplication> sortedApplications = applications.stream()
            .sorted((a1, a2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "createdAt" -> a1.getCreatedAt().compareTo(a2.getCreatedAt());
                    case "businessName" -> a1.getBusinessName().compareTo(a2.getBusinessName());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedApplications.size());
        
        if (start >= sortedApplications.size()) {
            return new PageImpl<>(List.of(), pageable, sortedApplications.size());
        }
        
        List<ProducerApplication> paginatedApplications = sortedApplications.subList(start, end);
        
        return new PageImpl<>(
            paginatedApplications.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList()),
            pageable,
            sortedApplications.size()
        );
    }

    public ProducerApplicationResponse getCustomerApplication(Long customerId) {
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Customer not found"));
        ProducerApplication application = applicationRepository.findByCustomer(customer)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "No application found for this customer"));
        return mapToDTO(application);
    }

    public ApplicationStatusResponse checkApplicationStatus(Long customerId) {
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Customer not found"));
            
        Optional<ProducerApplication> application = applicationRepository.findByCustomer(customer);
        
        if (application.isEmpty()) {
            return ApplicationStatusResponse.builder()
                .status("NO_APPLICATION")
                .build();
        }
        
        ProducerApplication app = application.get();
        ApplicationStatusResponse.ApplicationStatusResponseBuilder responseBuilder = ApplicationStatusResponse.builder()
            .status(app.getStatus().name())
            .submittedAt(app.getCreatedAt());
            
        // Add processed time and decline reason for processed applications
        if (app.getStatus() != ApplicationStatus.PENDING) {
            responseBuilder.processedAt(app.getUpdatedAt());
            if (app.getStatus() == ApplicationStatus.DECLINED) {
                responseBuilder.declineReason(app.getDeclineReason());
            }
        }
        
        return responseBuilder.build();
    }
}
