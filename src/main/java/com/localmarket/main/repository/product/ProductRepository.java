package com.localmarket.main.repository.product;

import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.product.ProductStatus;
import com.localmarket.main.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByProducer(User producer);
    Page<Product> findByProducer(User producer, Pageable pageable);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            JOIN FETCH p.producer
            JOIN FETCH p.categories c
            WHERE c.categoryId = :categoryId
            """)
    List<Product> findByCategoriesCategoryId(@Param("categoryId") Long categoryId);
    
    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            JOIN p.producer
            JOIN p.categories c
            WHERE c.categoryId = :categoryId
            """, 
            countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM Product p
            JOIN p.categories c
            WHERE c.categoryId = :categoryId
            """)
    Page<Product> findByCategoriesCategoryIdPaged(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            JOIN p.producer
            JOIN p.categories c
            WHERE c.categoryId = :categoryId AND p.status = :status
            """, 
            countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM Product p
            JOIN p.categories c
            WHERE c.categoryId = :categoryId AND p.status = :status
            """)
    Page<Product> findByCategoriesCategoryIdAndStatusPaged(
        @Param("categoryId") Long categoryId, 
        @Param("status") ProductStatus status, 
        Pageable pageable);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            JOIN FETCH p.producer
            LEFT JOIN FETCH p.categories cat
            LEFT JOIN FETCH cat.products
            """)
    List<Product> findAllWithCategories();
    
    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            JOIN p.producer
            LEFT JOIN p.categories cat
            """, 
            countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM Product p
            """)
    Page<Product> findAllWithCategoriesPaged(Pageable pageable);
    
    @Query(value = """
            SELECT DISTINCT p
            FROM Product p
            JOIN p.producer
            LEFT JOIN p.categories cat
            WHERE p.status = :status
            """, 
            countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM Product p
            WHERE p.status = :status
            """)
    Page<Product> findByStatusPaged(@Param("status") ProductStatus status, Pageable pageable);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            JOIN FETCH p.producer
            LEFT JOIN FETCH p.categories cat
            LEFT JOIN FETCH cat.products
            WHERE p.productId = :id
            """)
    Optional<Product> findByIdWithCategories(@Param("id") Long id);

    // Find distinct producers with approved products
    @Query(value = """
            SELECT DISTINCT p.producer
            FROM Product p
            WHERE p.status = :status
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.producer)
            FROM Product p
            WHERE p.status = :status
            """)
    Page<User> findDistinctProducersByProductStatus(@Param("status") ProductStatus status, Pageable pageable);
    
    // Find all approved products with their producers, for pagination
    @Query(value = """
            SELECT p
            FROM Product p
            WHERE p.status = :status
            """)
    Page<Product> findByStatusWithProducers(@Param("status") ProductStatus status, Pageable pageable);

    // Find products by producer and status
    List<Product> findByProducerAndStatus(User producer, ProductStatus status);

    List<Product> findByStatus(ProductStatus status);
    List<Product> findByProducerUserIdAndStatus(Long producerId, ProductStatus status);
    Page<Product> findByProducerUserIdAndStatus(Long producerId, ProductStatus status, Pageable pageable);
    
    List<Product> findByProducerUserId(Long producerId);
    Page<Product> findByProducerUserId(Long producerId, Pageable pageable);
    
    List<Product> findByProducerUserIdAndStatusIn(Long producerId, List<ProductStatus> statuses);
    Page<Product> findByProducerUserIdAndStatusIn(Long producerId, List<ProductStatus> statuses, Pageable pageable);

    // Find products by status with pagination
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    // Find products by status and name or description containing search term (case insensitive)
    @Query(value = """
            SELECT p FROM Product p
            WHERE p.status = :status
            AND (LOWER(p.name) LIKE :searchTerm
            OR LOWER(p.description) LIKE :searchTerm)
            """)
    Page<Product> findByStatusAndNameOrDescriptionContaining(
        @Param("status") ProductStatus status,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);
}