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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;
import java.util.List;
import java.math.BigDecimal;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {
    private final ProductService productService;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    @Operation(summary = "Create new product", description = "Create a new product (Producer only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product created successfully", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized as producer", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Get product by ID", description = "Retrieve product details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductByIdWithCategories(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, 
                "Product with id " + id + " not found")));
    }

    @Operation(summary = "Update product", description = "Update existing product (Producer only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully", content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized to update this product", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Delete product", description = "Delete existing product (Producer only)")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this product", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ProducerOnly
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        productService.deleteProduct(id, producerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all products", description = "Retrieve all products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found", content = @Content(schema = @Schema(implementation = ProducerProductsResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProducerProductsResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProductsGroupedByProducer());
    }

    @Operation(summary = "Get products by category", description = "Retrieve products by category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found", content = @Content(schema = @Schema(implementation = ProducerProductsResponse.class)))
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProducerProductsResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @Operation(summary = "Get product image", description = "Retrieve product image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image found", content = @Content(schema = @Schema(implementation = Resource.class)))
    })
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