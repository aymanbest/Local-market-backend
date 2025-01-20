package com.localmarket.main.controller.product;

import com.localmarket.main.entity.product.Product;
import com.localmarket.main.dto.product.ProductRequest;
import com.localmarket.main.service.product.ProductService;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final JwtService jwtService;

    @PostMapping
    @ProducerOnly
    public ResponseEntity<Product> createProduct(
            @RequestBody ProductRequest request,
            @RequestHeader("Authorization") String token) {
        String producerEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(productService.createProduct(request, producerEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PutMapping("/{id}")
    @ProducerOnly
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request,
            @RequestHeader("Authorization") String token) {
        String producerEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(productService.updateProduct(id, request, producerEmail));
    }

    @DeleteMapping("/{id}")
    @ProducerOnly
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        String producerEmail = jwtService.extractUsername(token.substring(7));
        productService.deleteProduct(id, producerEmail);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }
} 