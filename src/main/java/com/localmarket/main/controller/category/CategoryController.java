package com.localmarket.main.controller.category;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.dto.category.CategoryRequest;
import com.localmarket.main.service.category.CategoryService;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.dto.category.CategoryDTO;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final JwtService jwtService;

    @PostMapping
    @AdminOnly
    public ResponseEntity<CategoryDTO> createCategory(
            @RequestBody CategoryRequest request,
            @RequestHeader("Authorization") String token) {
        String adminEmail = jwtService.extractUsername(token.substring(7));
        return ResponseEntity.ok(categoryService.createCategory(request, adminEmail));
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
} 