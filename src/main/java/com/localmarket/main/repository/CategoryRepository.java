package com.localmarket.main.repository;

import com.localmarket.main.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.products")
    List<Category> findAllWithProducts();
    
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.products WHERE c.categoryId = :id")
    Optional<Category> findByIdWithProducts(@Param("id") Long id);
} 