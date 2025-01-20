package com.localmarket.main.service.category;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.dto.category.CategoryRequest;
import com.localmarket.main.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.user.Role;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.dto.category.CategoryDTO;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @AdminOnly
    public CategoryDTO createCategory(CategoryRequest request, String adminEmail) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Category name cannot be empty");
        }
        if (categoryRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            throw new ApiException(ErrorType.DUPLICATE_RESOURCE, 
                                 "Category with name '" + request.getName() + "' already exists");
        }

        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Admin not found"));
        
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only admins can create categories");
        }
        

        try {
            Category category = new Category();
            category.setName(request.getName().trim());
            Category savedCategory = categoryRepository.save(category);
            
            return CategoryDTO.builder()
                    .categoryId(savedCategory.getCategoryId())
                    .name(savedCategory.getName())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create category: " + e.getMessage());
        }
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