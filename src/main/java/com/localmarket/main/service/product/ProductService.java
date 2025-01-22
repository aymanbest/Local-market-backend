package com.localmarket.main.service.product;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.entity.user.Role;
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

import com.localmarket.main.dto.product.ProductDTO;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @ProducerOnly
    public Product createProduct(ProductRequest request, Long producerId) {
        User producer = userRepository.findById(producerId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Producer not found"));
            
        if (producer.getRole() != Role.PRODUCER) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only producers can create products");
        }

        try {
            Product product = new Product();
            product.setName(request.getName().trim());
            product.setDescription(request.getDescription());
            product.setPrice(request.getPrice());
            product.setQuantity(request.getQuantity());
            product.setImageUrl(request.getImageUrl());
            product.setProducer(producer);

            if (request.getCategoryIds() != null) {
                Set<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                    .stream().collect(Collectors.toSet());
                if (categories.size() != request.getCategoryIds().size()) {
                    throw new ApiException(ErrorType.RESOURCE_NOT_FOUND, "One or more categories not found");
                }
                product.setCategories(categories);
            }

            return productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.DUPLICATE_RESOURCE, "Product with similar details already exists");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create product: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllWithCategories();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findByIdWithCategories(id)
        .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, 
            "Product with id " + id + " not found"));
    }


    @ProducerOnly
    public Product updateProduct(Long id, ProductRequest request, Long producerId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, "Product not found"));
            
        if (!product.getProducer().getUserId().equals(producerId)) {
            throw new ApiException(ErrorType.PRODUCT_ACCESS_DENIED, "You don't have permission to modify this product");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());

        if (request.getCategoryIds() != null) {
            Set<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                .stream().collect(Collectors.toSet());
            product.setCategories(categories);
        }

        return productRepository.save(product);
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

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoriesCategoryId(categoryId);
    }

    private ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
            product.getProductId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getQuantity(),
            product.getImageUrl(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getProducer().getUserId(),
            product.getProducer().getUsername(),
            product.getProducer().getEmail(),
            product.getProducer().getRole(),
            product.getCategories()
        );
    }

    public List<ProductDTO> getAllProductsWithCategories() {
        return productRepository.findAllWithCategories().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public Optional<ProductDTO> getProductByIdWithCategories(Long id) {
        return productRepository.findByIdWithCategories(id)
            .map(this::convertToDTO);
    }
} 