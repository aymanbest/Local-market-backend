package com.localmarket.main.controller.product;


import com.localmarket.main.dto.product.ProductRequest;
import com.localmarket.main.service.product.ProductService;
import com.localmarket.main.security.ProducerOnly;
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
import com.localmarket.main.entity.product.ProductStatus;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.dto.product.ProductDeclineRequest;
import com.localmarket.main.dto.product.MyProductResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.localmarket.main.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {
    private final ProductService productService;
    private final FileStorageService fileStorageService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Operation(summary = "Create new product", description = "Create a new product (Producer only)")
    @SecurityRequirement(name = "cookie")
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
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

        return ResponseEntity.ok(productService.createProduct(request, image, userDetails.getId()));
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
    @SecurityRequirement(name = "cookie")
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
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

        return ResponseEntity.ok(productService.updateProduct(id, request, image, userDetails.getId()));
    }

    @Operation(summary = "Delete product", description = "Delete existing product (Producer only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this product", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ProducerOnly
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        productService.deleteProduct(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete product", description = "Delete existing product (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this product", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/admin/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteProductAsAdmin(@PathVariable Long id) {
        productService.deleteProductAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all products", description = "Retrieve all products with pagination and optional search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found", content = @Content(schema = @Schema(implementation = ProducerProductsResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ProducerProductsResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(productService.getAllProductsGroupedByProducer(pageable, search));
    }

    @Operation(summary = "Get products by category", description = "Retrieve products by category with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found", content = @Content(schema = @Schema(implementation = ProducerProductsResponse.class)))
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProducerProductsResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }

    @Operation(summary = "Get product image", description = "Retrieve product image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Image found", content = @Content(schema = @Schema(implementation = Resource.class)))
    })
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        Resource resource = fileStorageService.loadFileAsResource(filename);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(resource);
    }

    @Operation(summary = "Get pending products", description = "Get all pending products (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found"),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin")
    })
    @GetMapping("/pending")
    @AdminOnly
    public ResponseEntity<Page<ProducerProductsResponse>> getPendingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(productService.getProductsByStatus(ProductStatus.PENDING, pageable));
    }

    @Operation(summary = "Approve product", description = "Approve a pending product (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product approved"),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{id}/approve")
    @AdminOnly
    public ResponseEntity<ProductResponse> approveProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.updateProductStatus(id, ProductStatus.APPROVED, null));
    }

    @Operation(summary = "Decline product", description = "Decline a pending product (Admin only)")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product declined"),
        @ApiResponse(responseCode = "403", description = "Not authorized as admin"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{id}/decline")
    @AdminOnly
    public ResponseEntity<ProductResponse> declineProduct(
            @PathVariable Long id,
            @RequestBody ProductDeclineRequest request) {
        return ResponseEntity.ok(productService.updateProductStatus(id, ProductStatus.DECLINED, request.getReason()));
    }

    @GetMapping("/my-pending")
    @Operation(summary = "Get my pending and declined products", description = "Get producer's pending and declined products")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found"),
        @ApiResponse(responseCode = "403", description = "Not authorized as producer")
    })
    @ProducerOnly
    public ResponseEntity<Page<MyProductResponse>> getMyPendingProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(productService.getProducerPendingAndDeclinedProducts(userDetails.getId(), pageable));
    }

    @Operation(summary = "Get my products", description = "Get all products for the authenticated producer")
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products found"),
        @ApiResponse(responseCode = "403", description = "Not authorized as producer")
    })
    @GetMapping("/my-products")
    @ProducerOnly
    public ResponseEntity<Page<MyProductResponse>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(productService.getProducerProducts(userDetails.getId(), pageable));
    }
} 