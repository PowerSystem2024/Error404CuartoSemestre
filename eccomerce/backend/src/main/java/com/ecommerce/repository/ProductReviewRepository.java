package com.ecommerce.repository;

import com.ecommerce.model.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends BaseRepository<ProductReview, Long> {

    @Query("SELECT r FROM ProductReview r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product WHERE r.product.id = :productId AND r.status = 'APPROVED' AND r.deletedAt IS NULL AND r.active = true ORDER BY r.createdAt DESC")
    Page<ProductReview> findByProductIdAndStatusApprovedOrderByCreatedAtDesc(@Param("productId") Long productId,
            Pageable pageable);

    @Query("SELECT r FROM ProductReview r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product WHERE r.user.id = :userId AND r.deletedAt IS NULL AND r.active = true ORDER BY r.createdAt DESC")
    Page<ProductReview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT r FROM ProductReview r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product WHERE r.status <> 'APPROVED' AND r.deletedAt IS NULL AND r.active = true ORDER BY r.createdAt DESC")
    Page<ProductReview> findByStatusNotApproved(Pageable pageable);

    @Query("SELECT r FROM ProductReview r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.product WHERE r.product.id = :productId AND r.status = 'APPROVED' AND r.deletedAt IS NULL AND r.active = true")
    List<ProductReview> findByProductIdAndStatusApproved(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId AND r.status = 'APPROVED' AND r.deletedAt IS NULL AND r.active = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.id = :productId AND r.status = 'APPROVED' AND r.deletedAt IS NULL AND r.active = true")
    Long countApprovedReviewsByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) > 0 FROM ProductReview r WHERE r.product.id = :productId AND r.user.id = :userId AND r.deletedAt IS NULL AND r.active = true")
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT r FROM ProductReview r WHERE r.product.id = :productId AND r.user.id = :userId AND r.deletedAt IS NULL AND r.active = true")
    ProductReview findByProductIdAndUserId(Long productId, Long userId);
}