package com.ecommerce.service.impl;

import com.ecommerce.dto.request.ProductReviewRequest;
import com.ecommerce.dto.response.ProductReviewResponse;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.Product;
import com.ecommerce.model.entity.ProductReview;
import com.ecommerce.model.entity.Profile;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.model.enums.ReviewStatus;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductReviewRepository;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.interfaces.ProductReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final OrderRepository orderRepository;

    @Override
    public ProductReviewResponse createReview(ProductReviewRequest request, Long userId) {
        log.debug("Creando review de producto - ProductID: {}, User: {}", request.getProductId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con ID: " + request.getProductId()));

        // Verificar que el usuario no haya hecho ya una review de este producto
        if (productReviewRepository.existsByProductIdAndUserId(request.getProductId(), user.getId())) {
            throw new IllegalArgumentException("Ya has hecho una review de este producto");
        }

        // Verificar si el usuario ha comprado el producto para marcar como
        // verifiedPurchase
        boolean verifiedPurchase = hasUserPurchasedProduct(user.getId(), request.getProductId());

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .imageUrls(request.getImageUrls())
                .verifiedPurchase(verifiedPurchase)
                .status(ReviewStatus.APPROVED) // Las reviews se muestran inmediatamente
                .approvedAt(LocalDateTime.now()) // Fecha de aprobación automática
                .build();

        ProductReview savedReview = productReviewRepository.save(review);

        // log.info("Review creada exitosamente - ID: {}, ProductID: {}, User: {},
        // Email: {}",
        // savedReview.getId(), request.getProductId(), userId, user.getEmail());

        return mapToResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        log.debug("Obteniendo reviews de producto - ProductID: {}, Página: {}, Tamaño: {}",
                productId, pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductReview> reviews = productReviewRepository.findByProductIdAndStatusApprovedOrderByCreatedAtDesc(
                productId, pageable);

        Page<ProductReviewResponse> responses = reviews.map(this::mapToResponse);

        log.debug("Reviews obtenidas - ProductID: {}, Cantidad: {}", productId, responses.getNumberOfElements());

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        log.debug("Obteniendo reviews de usuario - User: {}, Página: {}, Tamaño: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        Page<ProductReview> reviews = productReviewRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), pageable);

        Page<ProductReviewResponse> responses = reviews.map(this::mapToResponse);

        log.debug("Reviews de usuario obtenidas - User: {}, Cantidad: {}", userId, responses.getNumberOfElements());

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getPendingReviews(Pageable pageable) {
        log.debug("Obteniendo reviews pendientes - Página: {}, Tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductReview> reviews = productReviewRepository.findByStatusNotApproved(pageable);

        Page<ProductReviewResponse> responses = reviews.map(this::mapToResponse);

        log.debug("Reviews pendientes obtenidas - Cantidad: {}", responses.getNumberOfElements());

        return responses;
    }

    @Override
    public ProductReviewResponse approveReview(Long reviewId) {
        log.debug("Aprobando review - ReviewID: {}", reviewId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        review.setStatus(ReviewStatus.APPROVED);
        review.setApprovedAt(LocalDateTime.now());

        ProductReview savedReview = productReviewRepository.save(review);

        // log.info("Review aprobada exitosamente - ID: {}", reviewId);

        return mapToResponse(savedReview);
    }

    @Override
    public void rejectReview(Long reviewId) {
        log.debug("Rechazando review - ReviewID: {}", reviewId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        productReviewRepository.delete(review);

        // log.info("Review rechazada y eliminada - ID: {}", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewStats getProductReviewStats(Long productId) {
        log.debug("Obteniendo estadísticas de reviews - ProductID: {}", productId);

        Double averageRating = productReviewRepository.getAverageRatingByProductId(productId);
        Long totalReviews = productReviewRepository.countApprovedReviewsByProductId(productId);

        // Calcular distribución de calificaciones
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }

        List<ProductReview> reviews = productReviewRepository.findByProductIdAndStatusApproved(productId);
        for (ProductReview review : reviews) {
            if (review.getRating() >= 1 && review.getRating() <= 5) {
                ratingDistribution.put(review.getRating(), ratingDistribution.get(review.getRating()) + 1);
            }
        }

        ProductReviewStats stats = new ProductReviewStats(averageRating, totalReviews, ratingDistribution);

        log.debug("Estadísticas obtenidas - ProductID: {}, Promedio: {}, Total: {}",
                productId, averageRating, totalReviews);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(Long productId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        return productReviewRepository.existsByProductIdAndUserId(productId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getUserReviewForProduct(Long productId, Long userId) {
        log.debug("Obteniendo review del usuario {} para producto {}", userId, productId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        ProductReview review = productReviewRepository.findByProductIdAndUserId(productId, user.getId());

        if (review == null) {
            throw new ResourceNotFoundException("Review no encontrada para este producto");
        }

        log.debug("Review encontrada - ID: {}, Usuario: {}", review.getId(), userId);

        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getReviewById(Long reviewId) {
        log.debug("Obteniendo review por ID: {}", reviewId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        return mapToResponse(review);
    }

    @Override
    public ProductReviewResponse updateReview(Long reviewId, ProductReviewRequest request, Long userId) {
        log.debug("Actualizando review - ReviewID: {}, User: {}", reviewId, userId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        // Verificar que el usuario sea el propietario de la review
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes permisos para editar esta review");
        }

        // Permitir actualizar reseñas propias (comentado el check de status aprobado)
        // if (review.getStatus() == ReviewStatus.APPROVED) {
        // throw new IllegalArgumentException("No se puede editar una review ya
        // aprobada");
        // }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setImageUrls(request.getImageUrls());

        ProductReview savedReview = productReviewRepository.save(review);

        // log.info("Review actualizada exitosamente - ID: {}", reviewId);

        return mapToResponse(savedReview);
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) {
        log.debug("Eliminando review (soft delete) - ReviewID: {}, User: {}", reviewId, userId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        // Verificar que el usuario sea el propietario de la review
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("No tienes permisos para eliminar esta review");
        }

        review.setActive(false);
        review.setDeletedAt(LocalDateTime.now());
        review.setDeletedBy(String.valueOf(userId));
        review.setUpdatedAt(LocalDateTime.now());
        productReviewRepository.save(review);

        // log.info("Review eliminada exitosamente - ID: {}, Producto: '{}', Eliminada
        // por: {}",
        // reviewId, review.getProduct().getName(), userId);
    }

    @Override
    public void deleteReviewWithReason(Long reviewId, Long adminId, String reason) {
        log.info("Admin ocultando review con razón - ReviewID: {}, AdminID: {}, Razón: {}", reviewId, adminId, reason);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        review.setActive(false);
        review.setDeletedAt(LocalDateTime.now());
        review.setDeletedBy(String.valueOf(adminId));
        review.setHiddenReason(reason);
        review.setUpdatedAt(LocalDateTime.now());
        productReviewRepository.save(review);

        log.info("Review ocultada exitosamente - ID: {}, Producto: '{}', Razón: '{}'",
                reviewId, review.getProduct().getName(), reason);
    }

    private ProductReviewResponse mapToResponse(ProductReview review) {
        // Intentar obtener el perfil del usuario para mostrar nombre completo
        Profile profile = null;
        try {
            profile = profileRepository.findByUserId(review.getUser().getId()).orElse(null);
        } catch (Exception e) {
            // Si hay problemas de lazy loading, continuar sin perfil
            log.debug("No se pudo cargar el perfil del usuario {}: {}", review.getUser().getId(), e.getMessage());
        }

        UserResponse user = UserResponse.builder()
                .id(review.getUser().getId())
                .email(review.getUser().getEmail())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .role(review.getUser().getRole())
                .build();

        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .user(user)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .imageUrls(review.getImageUrls())
                .verifiedPurchase(review.getVerifiedPurchase())
                .approved(review.getStatus() == ReviewStatus.APPROVED)
                .createdAt(review.getCreatedAt())
                .approvedAt(review.getApprovedAt())
                .build();
    }

    /**
     * Verifica si un usuario ha comprado un producto específico
     */
    private boolean hasUserPurchasedProduct(Long userId, Long productId) {
        log.debug("Verificando si usuario {} ha comprado producto {}", userId, productId);

        // Buscar órdenes completadas del usuario que contengan el producto
        Page<Order> userOrdersPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged());
        List<Order> userOrders = userOrdersPage.getContent();

        boolean hasPurchased = userOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED ||
                        order.getStatus() == OrderStatus.SHIPPED)
                .anyMatch(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(productId)));

        log.debug("Verificación de compra - Usuario: {}, Producto: {}, Resultado: {}", userId, productId, hasPurchased);

        return hasPurchased;
    }

    @Override
    public void hardDeleteReview(Long reviewId) {
        log.warn("Eliminando permanentemente review - ID: {}", reviewId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        productReviewRepository.delete(review);

        log.warn("Review eliminada permanentemente - ID: {}, Producto: '{}', Eliminada por: {}",
                review.getId(), review.getProduct().getName(), getCurrentUserEmail());
    }

    @Override
    public void hardDeleteUserReviews(Long userId, String adminIdentifier) {
        log.warn("Eliminando permanentemente todas las reseñas del usuario - UserID: {}, Admin: {}", userId,
                adminIdentifier);

        // Obtener todas las reseñas del usuario
        List<ProductReview> userReviews = productReviewRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .getContent();

        if (userReviews.isEmpty()) {
            // log.info("No se encontraron reseñas para el usuario - UserID: {}", userId);
            return;
        }

        // Eliminar cada reseña con auditoría
        for (ProductReview review : userReviews) {
            // Actualizar campos de auditoría
            review.setDeletedAt(LocalDateTime.now());
            review.setDeletedBy(adminIdentifier);
            productReviewRepository.save(review);

            // Eliminar permanentemente
            productReviewRepository.delete(review);

            log.warn("Reseña eliminada permanentemente - ReviewID: {}, ProductID: {}, UserID: {}",
                    review.getId(), review.getProduct().getId(), userId);
        }

        log.warn("Todas las reseñas del usuario eliminadas - UserID: {}, Total: {}", userId, userReviews.size());
    }

    @Override
    public void restoreReview(Long reviewId) {
        // log.info("Restaurando review - ID: {}", reviewId);

        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review no encontrada con ID: " + reviewId));

        if (review.getActive()) {
            throw new IllegalStateException("La review ya está activa");
        }

        review.setActive(true);
        review.setDeletedAt(null);
        review.setDeletedBy(null);
        review.setUpdatedAt(LocalDateTime.now());
        productReviewRepository.save(review);

        // log.info("Review restaurada exitosamente - ID: {}, Producto: '{}', Restaurada
        // por: {}",
        // review.getId(), review.getProduct().getName(), getCurrentUserEmail());
    }

    /**
     * Obtiene el email del usuario actualmente autenticado
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "system"; // Usuario por defecto si no hay autenticación
    }
}
