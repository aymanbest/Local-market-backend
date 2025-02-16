package com.localmarket.main.service.category;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.repository.category.CategoryRepository;
import com.localmarket.main.dto.category.CategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.dto.category.CategoryResponse;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import org.springframework.dao.DataIntegrityViolationException;
import com.localmarket.main.entity.product.ProductStatus;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.ArrayList;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.repository.product.ProductRepository;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;


    public CategoryResponse createCategory(CategoryRequest request) {
        try {
            Category category = new Category();
            category.setName(request.getName());
            
            Category savedCategory = categoryRepository.save(category);
            return CategoryResponse.builder()
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
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAllWithProducts();
        return categories.stream()
            .map(category -> CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .productCount(category.getProducts().stream()
                    .filter(product -> product.getStatus() == ProductStatus.APPROVED)
                    .collect(Collectors.toSet())
                    .size())
                .build())
            .collect(Collectors.toList());
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
        Category category = categoryRepository.findByIdWithProducts(id)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Category not found"));
            
        // Check if category has any approved or pending products
        boolean hasActiveProducts = category.getProducts().stream()
            .anyMatch(product -> product.getStatus() == ProductStatus.APPROVED || 
                                product.getStatus() == ProductStatus.PENDING);
            
        if (hasActiveProducts) {
            throw new ApiException(ErrorType.RESOURCE_IN_USE, 
                "Cannot delete category with existing approved or pending products");
        }

        categoryRepository.deleteById(id);
    }

    public void deleteCategoryWithProducts(Long id) {
        Category category = categoryRepository.findByIdWithProducts(id)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Category not found"));
        
        // Get all products that have this category
        Set<Product> productsToCheck = category.getProducts();
        
        // Separate products into those to delete and those to update
        List<Product> productsToDelete = new ArrayList<>();
        List<Product> productsToUpdate = new ArrayList<>();
        
        for (Product product : productsToCheck) {
            if (product.getCategories().size() <= 1) {
                // If this is the only category, mark for deletion regardless of status
                productsToDelete.add(product);
            } else {
                // If product has other categories, mark for update
                productsToUpdate.add(product);
            }
        }
        
        // Remove the category from products that have multiple categories
        for (Product product : productsToUpdate) {
            product.getCategories().remove(category);
            productRepository.save(product);
        }
        
        // Delete products that only had this category
        productRepository.deleteAll(productsToDelete);
        
        // Finally delete the category
        categoryRepository.deleteById(id);
    }

    public Category findByName(String name) {
        return categoryRepository.findByName(name)
            .orElseThrow(() -> new ApiException(ErrorType.CATEGORY_NOT_FOUND, 
                "Category with name " + name + " not found"));
    }
} 