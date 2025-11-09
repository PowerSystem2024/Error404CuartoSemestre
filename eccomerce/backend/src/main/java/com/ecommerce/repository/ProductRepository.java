package com.ecommerce.repository;

import com.ecommerce.model.entity.Product;
import com.ecommerce.model.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.deletedAt IS NULL")
        Page<Product> findAllProductsForAdmin(Pageable pageable);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.status = :status AND p.deletedAt IS NULL")
        Page<Product> findByStatusAndDeletedAtIsNull(ProductStatus status, Pageable pageable);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.category.id = :categoryId AND p.status = :status AND p.deletedAt IS NULL")
        Page<Product> findByCategoryIdAndStatusAndDeletedAtIsNull(Long categoryId, ProductStatus status,
                        Pageable pageable);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.status = :status AND p.deletedAt IS NULL")
        Page<Product> findByNameContainingIgnoreCaseAndStatusAndDeletedAtIsNull(String name, ProductStatus status,
                        Pageable pageable);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.featured = true AND p.status = :status AND p.deletedAt IS NULL ORDER BY COALESCE(p.featuredOrder, 999999) ASC, p.id DESC")
        List<Product> findByFeaturedTrueAndStatusAndDeletedAtIsNull(ProductStatus status);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.stockQuantity <= :threshold AND p.status = :status AND p.deletedAt IS NULL")
        List<Product> findByStockQuantityLessThanEqualAndStatusAndDeletedAtIsNull(Integer threshold,
                        ProductStatus status);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL AND "
                        +
                        "(LOWER(p.name) = LOWER(:search) OR " +
                        "LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) OR " +
                        "LOWER(p.name) LIKE LOWER(CONCAT('% ', :search, '%')) OR " +
                        "LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "ORDER BY " +
                        "CASE " +
                        "WHEN LOWER(p.name) = LOWER(:search) THEN 1 " +
                        "WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 2 " +
                        "WHEN LOWER(p.name) LIKE LOWER(CONCAT('% ', :search, '%')) THEN 3 " +
                        "WHEN LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) THEN 4 " +
                        "ELSE 5 " +
                        "END")
        Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id AND p.deletedAt IS NULL")
        Product findByIdWithImages(@Param("id") Long id);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.deletedAt IS NULL AND " +
                        "(LOWER(p.name) = LOWER(:search) OR " +
                        "LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) OR " +
                        "LOWER(p.name) LIKE LOWER(CONCAT('% ', :search, '%')) OR " +
                        "LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(COALESCE(p.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "ORDER BY " +
                        "CASE " +
                        "WHEN LOWER(p.name) = LOWER(:search) THEN 1 " +
                        "WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 2 " +
                        "WHEN LOWER(p.name) LIKE LOWER(CONCAT('% ', :search, '%')) THEN 3 " +
                        "WHEN LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) THEN 4 " +
                        "ELSE 5 " +
                        "END")
        Page<Product> searchProductsForAdmin(@Param("search") String search, Pageable pageable);

        @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.id <> :id AND p.deletedAt IS NULL")
        boolean existsBySkuAndIdNot(String sku, Long id);
}
