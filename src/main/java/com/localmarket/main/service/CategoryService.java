package com.localmarket.main.service;

import com.localmarket.main.entity.Category;
import com.localmarket.main.repository.CategoryRepository;
import com.localmarket.main.repository.UserRepository;
import com.localmarket.main.dto.CategoryRequest;
import com.localmarket.main.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.localmarket.main.entity.User;
import com.localmarket.main.entity.Role;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @AdminOnly
    public Category createCategory(CategoryRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can create categories");
        }

        Category category = new Category();
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllWithProducts();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findByIdWithProducts(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @AdminOnly
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    @AdminOnly
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.delete(category);
    }
} 