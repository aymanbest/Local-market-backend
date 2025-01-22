package com.localmarket.main.service.category;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.dto.category.CategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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


    public CategoryDTO createCategory(CategoryRequest request) {
        try {
            Category category = new Category();
            category.setName(request.getName());
            
            Category savedCategory = categoryRepository.save(category);
            return CategoryDTO.builder()
                .categoryId(savedCategory.getCategoryId())
                .name(savedCategory.getName())
                .productCount(0)
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
        return categoryRepository.findByIdWithProducts(id)
            .orElseThrow(() -> new ApiException(ErrorType.CATEGORY_NOT_FOUND, 
                "Category with id " + id + " not found"));
    }

    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.CATEGORY_NOT_FOUND, "Category not found"));
            
        category.setName(request.getName());
        
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Category not found"));
            
        if (!category.getProducts().isEmpty()) {
            throw new ApiException(ErrorType.RESOURCE_IN_USE, "Cannot delete category with existing products");
        }

        categoryRepository.deleteById(id);
    }

    public Category findByName(String name) {
        return categoryRepository.findByName(name)
            .orElseThrow(() -> new ApiException(ErrorType.CATEGORY_NOT_FOUND, 
                "Category with name " + name + " not found"));
    }
} 