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



import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryDTO createCategory(CategoryRequest request, Long adminId) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
            
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can create categories");
        }

        Category category = new Category();
        category.setName(request.getName());
        
        Category savedCategory = categoryRepository.save(category);
        return CategoryDTO.builder()
            .categoryId(savedCategory.getCategoryId())
            .name(savedCategory.getName())
            .productCount(0)  // New category has no products
            .build();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public Category updateCategory(Long id, CategoryRequest request, Long adminId) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
            
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can update categories");
        }

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
            
        category.setName(request.getName());
        
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id, Long adminId) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
            
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can delete categories");
        }

        categoryRepository.deleteById(id);
    }
} 