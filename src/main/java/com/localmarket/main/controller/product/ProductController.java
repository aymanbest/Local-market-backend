package com.localmarket.main.controller.product;

import com.localmarket.main.entity.product.Product;
import com.localmarket.main.dto.product.ProductRequest;
import com.localmarket.main.service.product.ProductService;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.localmarket.main.dto.product.ProducerProductsResponse;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.dto.product.ProductResponse;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import com.localmarket.main.service.storage.FileStorageService;

import java.util.List;
import java.math.BigDecimal;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ProducerOnly
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart(value = "name", required = true) String name,
            @RequestPart(value = "description", required = true) String description,
            @RequestPart(value = "price", required = true) String priceStr,
            @RequestPart(value = "quantity", required = true) String quantityStr,
            @RequestPart(value = "categoryIds", required = true) String categoryIdsString,
            @RequestPart(value = "imageUrl", required = false) String imageUrl,
            @RequestPart(value = "image", required = false) MultipartFile[] images,
            @RequestHeader("Authorization") String token) {
        
        MultipartFile image = null;
        if (images != null && images.length > 0) {
            if (images.length > 1) {
                throw new ApiException(ErrorType.INVALID_FILE, "Only one image file is allowed");
            }
            image = images[0];
        }
        
        if (image == null && (imageUrl == null || imageUrl.trim().isEmpty())) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Either image file or imageUrl must be provided");
        }
        
        BigDecimal price = new BigDecimal(priceStr);
        Integer quantity = Integer.parseInt(quantityStr);
        
        Set<Long> categoryIds = Arrays.stream(categoryIdsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        
        ProductRequest request = ProductRequest.builder()
                .name(name)
                .description(description)
                .price(price)
                .quantity(quantity)
                .categoryIds(categoryIds)
                .imageUrl(imageUrl)
                .build();
                
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(productService.createProduct(request, image, producerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductByIdWithCategories(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, 
                "Product with id " + id + " not found")));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ProducerOnly
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "name", required = true) String name,
            @RequestPart(value = "description", required = true) String description,
            @RequestPart(value = "price", required = true) String priceStr,
            @RequestPart(value = "quantity", required = true) String quantityStr,
            @RequestPart(value = "categoryIds", required = true) String categoryIdsString,
            @RequestPart(value = "imageUrl", required = false) String imageUrl,
            @RequestPart(value = "image", required = false) MultipartFile[] images,
            @RequestHeader("Authorization") String token) {
        
        MultipartFile image = null;
        if (images != null && images.length > 0) {
            if (images.length > 1) {
                throw new ApiException(ErrorType.INVALID_FILE, "Only one image file is allowed");
            }
            image = images[0];
        }
        
        if (image == null && (imageUrl == null || imageUrl.trim().isEmpty())) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Either image file or imageUrl must be provided");
        }
        
        BigDecimal price = new BigDecimal(priceStr);
        Integer quantity = Integer.parseInt(quantityStr);
        
        Set<Long> categoryIds = Arrays.stream(categoryIdsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        
        ProductRequest request = ProductRequest.builder()
                .name(name)
                .description(description)
                .price(price)
                .quantity(quantity)
                .categoryIds(categoryIds)
                .imageUrl(imageUrl)
                .build();
                
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(productService.updateProduct(id, request, image, producerId));
    }

    @DeleteMapping("/{id}")
    @ProducerOnly
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        productService.deleteProduct(id, producerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProducerProductsResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProductsGroupedByProducer());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProducerProductsResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        Path imagePath = fileStorageService.getUploadPath().resolve(filename);
        Resource resource = new UrlResource(imagePath.toUri());
        
        if (resource.exists()) {
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 