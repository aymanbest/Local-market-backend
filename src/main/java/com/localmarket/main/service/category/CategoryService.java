package com.localmarket.main.service.category;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.dto.category.CategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.entity.user.Role;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.dto.category.CategoryDTO;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryDTO createCategory(CategoryRequest request, Long adminId) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Admin not found"));
            
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only admins can create categories");
        }

        try {
            Category category = new Category();
            category.setName(request.getName());
            
            Category savedCategory = categoryRepository.save(category);
            return CategoryDTO.builder()
                .categoryId(savedCategory.getCategoryId())
                .name(savedCategory.getName())
                .productCount(0)  // New category has no products
                .build();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.CATEGORY_ALREADY_EXISTS, 
                "Category with name '" + request.getName() + "' already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, 
                "Category with id " + id + " not found"));
    }

    public Category updateCategory(Long id, CategoryRequest request, Long adminId) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Admin not found"));
            
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only admins can update categories");
        }

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.CATEGORY_NOT_FOUND, "Category not found"));
            
        category.setName(request.getName());
        
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id, Long adminId) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Admin not found"));
            
        if (admin.getRole() != Role.ADMIN) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only admins can delete categories");
        }
        
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Category not found"));
            
        if (!category.getProducts().isEmpty()) {
            throw new ApiException(ErrorType.RESOURCE_IN_USE, "Cannot delete category with existing products");
        }

        categoryRepository.deleteById(id);
    }
} 