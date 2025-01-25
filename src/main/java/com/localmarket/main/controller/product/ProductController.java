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

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final JwtService jwtService;

    @PostMapping
    @ProducerOnly
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody ProductRequest request,
            @RequestHeader("Authorization") String token) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(productService.createProduct(request, producerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductByIdWithCategories(id)
            .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, 
                "Product with id " + id + " not found")));
    }

    @PutMapping("/{id}")
    @ProducerOnly
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request,
            @RequestHeader("Authorization") String token) {
        Long producerId = jwtService.extractUserId(token.substring(7));
        return ResponseEntity.ok(productService.updateProduct(id, request, producerId));
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
} 