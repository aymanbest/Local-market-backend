package com.localmarket.main.service;

import com.localmarket.main.entity.Category;
import com.localmarket.main.entity.Product;
import com.localmarket.main.entity.User;
import com.localmarket.main.repository.CategoryRepository;
import com.localmarket.main.repository.ProductRepository;
import com.localmarket.main.repository.UserRepository;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.entity.Role;
import com.localmarket.main.dto.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

import com.localmarket.main.dto.ProductDTO;


@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @ProducerOnly
    public Product createProduct(ProductRequest request, String producerEmail) {
        User producer = userRepository.findByEmail(producerEmail)
            .orElseThrow(() -> new RuntimeException("Producer not found"));
        
        if (producer.getRole() != Role.PRODUCER) {
            throw new RuntimeException("Only producers can create products");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setProducer(producer);

        if (request.getCategoryIds() != null) {
            Set<Category> categories = categoryRepository.findAllById(request.getCategoryIds())
                .stream().collect(Collectors.toSet());
            product.setCategories(categories);
        }

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllWithCategories();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findByIdWithCategories(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    }


    @ProducerOnly
    public Product updateProduct(Long id, ProductRequest request, String producerEmail) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
            
        if (!product.getProducer().getEmail().equals(producerEmail)) {
            throw new RuntimeException("You can only update your own products");
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
    public void deleteProduct(Long id, String producerEmail) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
            
        if (!product.getProducer().getEmail().equals(producerEmail)) {
            throw new RuntimeException("You can only delete your own products");
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