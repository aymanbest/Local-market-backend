package com.localmarket.main.repository;

import com.localmarket.main.entity.Product;
import com.localmarket.main.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProducer(User producer);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            JOIN FETCH p.producer
            JOIN FETCH p.categories c
            WHERE c.categoryId = :categoryId
            """)
    List<Product> findByCategoriesCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            JOIN FETCH p.producer
            LEFT JOIN FETCH p.categories cat
            LEFT JOIN FETCH cat.products
            """)
    List<Product> findAllWithCategories();

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            JOIN FETCH p.producer
            LEFT JOIN FETCH p.categories cat
            LEFT JOIN FETCH cat.products
            WHERE p.productId = :id
            """)
    Optional<Product> findByIdWithCategories(@Param("id") Long id);
}