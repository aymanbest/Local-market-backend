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
import com.localmarket.main.service.storage.FileStorageService;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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
                product.setImageUrl("/api/products/images/" + filename);
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

            return convertToDTO(productRepository.save(product));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.DUPLICATE_RESOURCE, "Product with similar details already exists");
        }
    }

    @ProducerOnly
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

        return convertToDTO(productRepository.save(product));
    }

    @ProducerOnly
    public void deleteProduct(Long id, Long producerId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
            
        if (!product.getProducer().getUserId().equals(producerId)) {
            throw new ApiException(ErrorType.PRODUCT_ACCESS_DENIED, "You can only delete your own products");
        }

        productRepository.delete(product);
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
        
        return new ProductResponse(
            product.getProductId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getQuantity(),
            product.getImageUrl(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            producerDTO,
            product.getCategories()
        );
    }

    @Transactional(readOnly = true)
    public List<ProducerProductsResponse> getAllProductsGroupedByProducer() {
        List<Product> allProducts = productRepository.findAllWithCategories();
        return groupProductsByProducer(allProducts);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductByIdWithCategories(Long id) {
        return productRepository.findByIdWithCategories(id)
            .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<ProducerProductsResponse> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ApiException(ErrorType.CATEGORY_NOT_FOUND, 
                "Category with id " + categoryId + " not found");
        }
        
        List<Product> products = productRepository.findByCategoriesCategoryId(categoryId);
        return groupProductsByProducer(products);
    }

    private List<ProducerProductsResponse> groupProductsByProducer(List<Product> products) {
        Map<User, List<Product>> groupedProducts = products.stream()
            .collect(Collectors.groupingBy(Product::getProducer));
        
        return groupedProducts.entrySet().stream()
            .map(entry -> {
                User producer = entry.getKey();
                List<ProductResponse> productResponses = entry.getValue().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
                    
                return new ProducerProductsResponse(
                    producer.getUserId(),
                    producer.getUsername(),
                    producer.getFirstname(),
                    producer.getLastname(),
                    producer.getEmail(),
                    productResponses
                );
            })
            .collect(Collectors.toList());
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
} 