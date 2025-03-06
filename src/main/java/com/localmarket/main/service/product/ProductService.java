package com.localmarket.main.service.product;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.dto.product.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;

import com.localmarket.main.dto.product.ProductResponse;
import java.math.BigDecimal;
import java.util.Map;
import com.localmarket.main.dto.product.ProducerProductsResponse;
import com.localmarket.main.dto.user.FilterUsersResponse;
import com.localmarket.main.service.notification.producer.ProducerNotificationService;
import com.localmarket.main.service.storage.FileStorageService;
import org.springframework.web.multipart.MultipartFile;
import com.localmarket.main.entity.product.ProductStatus;
import com.localmarket.main.dto.product.MyProductResponse;
import com.localmarket.main.entity.product.StockReservation;
import com.localmarket.main.repository.product.StockReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderItem;
import com.localmarket.main.dto.notification.NotificationResponse;
import com.localmarket.main.dto.review.VerifiedReviews;
import com.localmarket.main.repository.review.ReviewRepository;
import com.localmarket.main.entity.review.ReviewStatus;
import com.localmarket.main.entity.review.Review;
import com.localmarket.main.service.notification.admin.AdminNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.HashMap;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProducerNotificationService producerNotificationService;
    private final StockReservationRepository stockReservationRepository;
    private final ReviewRepository reviewRepository;
    private final AdminNotificationService adminNotificationService;
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int CRITICAL_STOCK_THRESHOLD = 5;

    @ProducerOnly
    public ProductResponse createProduct(ProductRequest request, MultipartFile image, Long producerId) {
        try {
            validateProductPrice(request.getPrice());
            
            Product product = new Product();
            product.setName(request.getName().trim());
            product.setDescription(request.getDescription());
            product.setPrice(request.getPrice());
            product.setQuantity(request.getQuantity());
            
            // Handle image: prefer uploaded file over URL
            if (image != null && !image.isEmpty()) {
                String filename = fileStorageService.storeFile(image);
                product.setImageUrl(filename);
            } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
                product.setImageUrl(request.getImageUrl().trim());
            }

            User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Producer not found"));
            product.setProducer(producer);

            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                Set<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                    .stream().collect(Collectors.toSet());
                if (categories.size() != request.getCategoryIds().size()) {
                    throw new ApiException(ErrorType.RESOURCE_NOT_FOUND, "One or more categories not found");
                }
                product.setCategories(categories);
            }

            product.setStatus(ProductStatus.PENDING);
            Product savedProduct = productRepository.save(product);

            adminNotificationService.notifyNewProductNeedsReview(savedProduct);

            return convertToDTO(savedProduct);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.DUPLICATE_RESOURCE, "Product with similar details already exists");
        }
    }

    @ProducerOnly
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, MultipartFile image, Long producerId) {
        validateProductPrice(request.getPrice());
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
            
        if (!product.getProducer().getUserId().equals(producerId)) {
            throw new ApiException(ErrorType.PRODUCT_ACCESS_DENIED, "You can only update your own products");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        if (image != null) {
            String filename = fileStorageService.storeFile(image);
            product.setImageUrl("/api/products/images/" + filename);
        } else if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                .stream().collect(Collectors.toSet());
            product.setCategories(categories);
        }

        product.setStatus(ProductStatus.PENDING);

        return convertToDTO(productRepository.save(product));
    }

    @Transactional
    public void deleteProductAsAdmin(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
        
        // Remove stock reservations first
        List<StockReservation> stockReservations = stockReservationRepository.findByProduct(product);
        if (!stockReservations.isEmpty()) {
            stockReservationRepository.deleteAll(stockReservations);
            stockReservationRepository.flush();
        }
        
        // Remove reviews
        List<Review> reviews = reviewRepository.findByProductProductId(id);
        if (!reviews.isEmpty()) {
            reviews.forEach(review -> review.setProduct(null));
            reviewRepository.saveAll(reviews);
        }
        
        // Remove categories
        product.setCategories(new HashSet<>());
        productRepository.save(product);
        
        // Now safe to delete
        productRepository.delete(product);
        productRepository.flush();
    }

    @ProducerOnly
    @Transactional
    public void deleteProduct(Long id, Long producerId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
            
        if (!product.getProducer().getUserId().equals(producerId)) {
            throw new ApiException(ErrorType.PRODUCT_ACCESS_DENIED, "You can only delete your own products");
        }

        // Remove stock reservations first
        List<StockReservation> stockReservations = stockReservationRepository.findByProduct(product);
        if (!stockReservations.isEmpty()) {
            stockReservationRepository.deleteAll(stockReservations);
            stockReservationRepository.flush();
        }
        
        // Remove reviews
        List<Review> reviews = reviewRepository.findByProductProductId(id);
        if (!reviews.isEmpty()) {
            reviews.forEach(review -> review.setProduct(null));
            reviewRepository.saveAll(reviews);
        }
        
        // Remove categories
        product.setCategories(new HashSet<>());
        productRepository.save(product);

        // Now safe to delete
        productRepository.delete(product);
        productRepository.flush();
    }

    private ProductResponse convertToDTO(Product product) {
        User producer = product.getProducer();
        FilterUsersResponse producerDTO = new FilterUsersResponse(
            producer.getUserId(),
            producer.getUsername(),
            producer.getFirstname(),
            producer.getLastname(),
            producer.getEmail()
        );
        
        List<VerifiedReviews> verifiedReviews = reviewRepository.findByProductProductId(product.getProductId())
            .stream()
            .filter(review -> review.isVerifiedPurchase() && review.getStatus() == ReviewStatus.APPROVED)
            .map((Review review) -> VerifiedReviews.builder()
                .reviewId(review.getReviewId())
                .customerUsername(review.getCustomer().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        ProductResponse response = new ProductResponse();
        response.setProductId(product.getProductId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setImageUrl(product.getImageUrl());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setCategories(product.getCategories());
        response.setProducer(producerDTO);
        response.setVerifiedReviews(verifiedReviews);
        response.setStock(product.getQuantity() > 0);
        
        return response;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, String searchTerm) {
        Page<Product> productPage;
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // For search queries, we use a native query with LIKE 7ssn fl performance doesnt lag
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            productPage = productRepository.findByStatusAndNameOrDescriptionContaining(
                ProductStatus.APPROVED, searchPattern, pageable);
        } else {
            productPage = productRepository.findByStatus(ProductStatus.APPROVED, pageable);
        }
        
        // Convert to ProductResponse
        return productPage.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductByIdWithCategories(Long id) {
        return productRepository.findByIdWithCategories(id)
            .filter(product -> product.getStatus() == ProductStatus.APPROVED)
            .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProducerProductsResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByCategoriesCategoryIdPaged(categoryId, pageable);
        
        // Group by producer and convert to response
        Map<User, List<ProductResponse>> groupedProducts = new HashMap<>();
        
        for (Product product : productPage.getContent()) {
            if (product.getStatus() == ProductStatus.APPROVED) {
                User producer = product.getProducer();
                ProductResponse productResponse = convertToDTO(product);
                
                groupedProducts.computeIfAbsent(producer, k -> new ArrayList<>())
                    .add(productResponse);
            }
        }
        
        List<ProducerProductsResponse> responses = groupedProducts.entrySet().stream()
            .map(entry -> new ProducerProductsResponse(
                entry.getKey().getUserId(),
                entry.getKey().getUsername(),
                entry.getKey().getFirstname(),
                entry.getKey().getLastname(),
                entry.getKey().getEmail(),
                entry.getValue()
            ))
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, productPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategoryFlat(Long categoryId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByCategoriesCategoryIdAndStatusPaged(
            categoryId, ProductStatus.APPROVED, pageable);
        
        List<ProductResponse> productResponses = productPage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(productResponses, pageable, productPage.getTotalElements());
    }

    private void validateProductPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorType.INVALID_PRODUCT_PRICE, 
                "Product price must be greater than 0");
        }
        
        if (price.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new ApiException(ErrorType.INVALID_PRODUCT_PRICE, 
                "Product price cannot exceed 999999.99");
        }
        
        if (price.scale() > 2) {
            throw new ApiException(ErrorType.INVALID_PRODUCT_PRICE, 
                "Product price cannot have more than 2 decimal places");
        }
    }

    @Transactional(readOnly = true)
    public List<ProducerProductsResponse> getProductsByStatus(ProductStatus status) {
        List<Product> products = productRepository.findByStatus(status);
        
        // Group by producer
        Map<User, List<Product>> groupedProducts = products.stream()
            .collect(Collectors.groupingBy(Product::getProducer));

        return groupedProducts.entrySet().stream()
            .map(entry -> new ProducerProductsResponse(
                entry.getKey().getUserId(),
                entry.getKey().getUsername(),
                entry.getKey().getFirstname(),
                entry.getKey().getLastname(),
                entry.getKey().getEmail(),
                entry.getValue().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ProducerProductsResponse> getProductsByStatus(ProductStatus status, Pageable pageable) {
        Page<Product> productPage = productRepository.findByStatusPaged(status, pageable);
        
        // Group by producer and convert to response
        Map<User, List<ProductResponse>> groupedProducts = new HashMap<>();
        
        for (Product product : productPage.getContent()) {
            User producer = product.getProducer();
            ProductResponse productResponse = convertToDTO(product);
            
            groupedProducts.computeIfAbsent(producer, k -> new ArrayList<>())
                .add(productResponse);
        }
        
        List<ProducerProductsResponse> responses = groupedProducts.entrySet().stream()
            .map(entry -> new ProducerProductsResponse(
                entry.getKey().getUserId(),
                entry.getKey().getUsername(),
                entry.getKey().getFirstname(),
                entry.getKey().getLastname(),
                entry.getKey().getEmail(),
                entry.getValue()
            ))
            .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, productPage.getTotalElements());
    }

    public List<ProductResponse> getProducerProductsByStatus(Long producerId, ProductStatus status) {
        return productRepository.findByProducerUserIdAndStatus(producerId, status)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse updateProductStatus(Long productId, ProductStatus status, String declineReason) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
            
        if (status == ProductStatus.DECLINED && (declineReason == null || declineReason.trim().isEmpty())) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Decline reason is required");
        }
        
        product.setStatus(status);
        product.setDeclineReason(declineReason);

        switch (status) {
            case APPROVED:
                producerNotificationService.notifyProductApproval(product.getProducer().getUserId(), product, true, null);
                break;
            case DECLINED:
                producerNotificationService.notifyProductApproval(product.getProducer().getUserId(), product, false, declineReason);
                break;
            default:
                break;
        }

        return convertToDTO(productRepository.save(product));
    }

    private MyProductResponse convertToMyProductDTO(Product product) {
        boolean stock = product.getQuantity() > 0;
        return new MyProductResponse(
            product.getProductId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getQuantity(),
            product.getImageUrl(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getCategories(),
            product.getStatus(),
            product.getDeclineReason(),
            stock
        );
    }

    @Transactional(readOnly = true)
    public Page<MyProductResponse> getProducerProducts(Long producerId, Pageable pageable) {
        User producer = userRepository.findById(producerId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                "Producer not found with id: " + producerId));
        
        Page<Product> productPage = productRepository.findByProducer(producer, pageable);
        
        return productPage.map(this::convertToMyProductDTO);
    }

    @Transactional(readOnly = true)
    public Page<MyProductResponse> getProducerPendingAndDeclinedProducts(Long producerId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByProducerUserIdAndStatusIn(
            producerId, 
            List.of(ProductStatus.PENDING, ProductStatus.DECLINED),
            pageable
        );
        
        return productPage.map(this::convertToMyProductDTO);
    }


    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void monitorStockLevels() {
        List<Product> products = productRepository.findAll();
        
        for (Product product : products) {
            int availableStock = getAvailableStock(product);
            int reservedStock = getReservedStock(product);
            int totalStock = availableStock + reservedStock;
            
            // Critical stock notification
            if (availableStock <= CRITICAL_STOCK_THRESHOLD) {
                NotificationResponse notification = NotificationResponse.builder()
                    .type("CRITICAL_STOCK_ALERT")
                    .message("CRITICAL ALERT: " + product.getName() + " stock is critically low!")
                    .data(Map.of(
                        "productId", product.getProductId(),
                        "productName", product.getName(),
                        "availableStock", availableStock,
                        "reservedStock", reservedStock,
                        "totalStock", totalStock
                    ))
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();
                
                producerNotificationService.sendToUser(
                    product.getProducer().getUserId(),
                    notification
                );
            }
            // Low stock notification
            else if (availableStock <= LOW_STOCK_THRESHOLD) {
                NotificationResponse notification = NotificationResponse.builder()
                    .type("LOW_STOCK_ALERT")
                    .message("Alert: " + product.getName() + " stock is running low")
                    .data(Map.of(
                        "productId", product.getProductId(),
                        "productName", product.getName(),
                        "availableStock", availableStock,
                        "reservedStock", reservedStock,
                        "totalStock", totalStock
                    ))
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();
                
                producerNotificationService.sendToUser(
                    product.getProducer().getUserId(),
                    notification
                );
            }
            
            // Stock movement notification (when reserved stock changes)
            if (reservedStock > 0) {
                NotificationResponse notification = NotificationResponse.builder()
                    .type("STOCK_MOVEMENT")
                    .message("Stock movement detected for " + product.getName())
                    .data(Map.of(
                        "productId", product.getProductId(),
                        "productName", product.getName(),
                        "availableStock", availableStock,
                        "reservedStock", reservedStock,
                        "totalStock", totalStock
                    ))
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();
                
                producerNotificationService.sendToUser(
                    product.getProducer().getUserId(),
                    notification
                );
            }
        }
    }

    private int getReservedStock(Product product) {
        LocalDateTime now = LocalDateTime.now();
        return stockReservationRepository
            .findByProductAndExpiresAtGreaterThan(product, now)
            .stream()
            .mapToInt(StockReservation::getQuantity)
            .sum();
    }

    @Transactional
    public void confirmStockReduction(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newQuantity = product.getQuantity() - item.getQuantity();
            product.setQuantity(newQuantity);
            
            NotificationResponse notification = NotificationResponse.builder()
                .type("STOCK_UPDATED")
                .message("Stock reduced for " + product.getName())
                .data(Map.of(
                    "productId", product.getProductId(),
                    "productName", product.getName(),
                    "previousQuantity", product.getQuantity(),
                    "newQuantity", newQuantity,
                    "reduction", item.getQuantity(),
                    "orderId", order.getOrderId()
                ))
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();
            
            producerNotificationService.sendToUser(
                product.getProducer().getUserId(),
                notification
            );
            
            productRepository.save(product);
        }
        stockReservationRepository.deleteByOrder(order);
    }

    @Transactional
    public void reserveStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int availableStock = getAvailableStock(product);
            
            if (availableStock < item.getQuantity()) {
                throw new ApiException(ErrorType.INSUFFICIENT_STOCK, 
                    "Insufficient stock for product: " + product.getName());
            }
            
            StockReservation reservation = new StockReservation();
            reservation.setProduct(product);
            reservation.setOrder(order);
            reservation.setQuantity(item.getQuantity());
            stockReservationRepository.save(reservation);
        }
    }

    @Transactional
    public void releaseStock(Order order) {
        stockReservationRepository.deleteByOrder(order);
    }

    public int getAvailableStock(Product product) {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> activeReservations = 
            stockReservationRepository.findByProductAndExpiresAtGreaterThan(product, now);
            
        int reservedQuantity = activeReservations.stream()
            .mapToInt(StockReservation::getQuantity)
            .sum();
            
        return product.getQuantity() - reservedQuantity;
    }
} 