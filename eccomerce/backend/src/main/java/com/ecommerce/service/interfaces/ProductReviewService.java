package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.ProductReviewRequest;
import com.ecommerce.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ProductReviewService {

    /**
     * Crea una nueva review de producto
     */
    ProductReviewResponse createReview(ProductReviewRequest request, Long userId);

    /**
     * Obtiene reviews aprobadas de un producto
     */
    Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable);

    /**
     * Obtiene reviews de un usuario
     */
    Page<ProductReviewResponse> getUserReviews(Long userId, Pageable pageable);

    /**
     * Obtiene reviews pendientes de aprobación
     */
    Page<ProductReviewResponse> getPendingReviews(Pageable pageable);

    /**
     * Aprueba una review
     */
    ProductReviewResponse approveReview(Long reviewId);

    /**
     * Rechaza una review
     */
    void rejectReview(Long reviewId);

    /**
     * Verifica si un usuario ya ha hecho una review de un producto
     */
    boolean hasUserReviewedProduct(Long productId, Long userId);

    /**
     * Obtiene la review de un usuario para un producto específico
     */
    ProductReviewResponse getUserReviewForProduct(Long productId, Long userId);

    /**
     * Obtiene una review por ID
     */
    ProductReviewResponse getReviewById(Long reviewId);

    /**
     * Actualiza una review
     */
    ProductReviewResponse updateReview(Long reviewId, ProductReviewRequest request, Long userId);

    /**
     * Elimina una review
     */
    void deleteReview(Long reviewId, Long userId);

    /**
     * Oculta una review con razón registrada (soft delete con auditoría)
     */
    void deleteReviewWithReason(Long reviewId, Long adminId, String reason);

    /**
     * Elimina permanentemente una review (hard delete)
     */
    void hardDeleteReview(Long reviewId);

    /**
     * Elimina permanentemente todas las reseñas de un usuario (para eliminación en
     * cascada)
     */
    void hardDeleteUserReviews(Long userId, String adminIdentifier);

    /**
     * Restaura una review previamente eliminada
     */
    void restoreReview(Long reviewId);

    // Clase interna para estadísticas
    record ProductReviewStats(Double averageRating, Long totalReviews, Map<Integer, Long> ratingDistribution) {
    }

    ProductReviewStats getProductReviewStats(Long productId);
}