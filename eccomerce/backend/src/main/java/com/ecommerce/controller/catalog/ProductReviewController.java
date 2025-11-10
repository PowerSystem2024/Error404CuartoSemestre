package com.ecommerce.controller.catalog;

import com.ecommerce.dto.request.ProductReviewRequest;
import com.ecommerce.dto.response.ProductReviewResponse;
import com.ecommerce.security.jwt.JwtUtils;
import com.ecommerce.service.interfaces.ProductReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/products/{productId}/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews de Productos", description = "Endpoints para gestión de reviews de productos")
public class ProductReviewController {

    private final ProductReviewService productReviewService;
    private final JwtUtils jwtUtils;

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtils.getUserIdFromJwtToken(token);
        }
        throw new RuntimeException("Token no encontrado");
    }

    @GetMapping
    @Operation(summary = "Obtener reviews de producto", description = "Obtiene las reviews aprobadas de un producto")
    public ResponseEntity<Page<ProductReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");

        log.info("Obteniendo reviews de producto - ProductID: {}, Página: {}, Tamaño: {}", productId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductReviewResponse> reviews = productReviewService.getProductReviews(productId, pageable);

            log.info("Reviews obtenidas exitosamente - ProductID: {}, Cantidad: {}", productId,
                    reviews.getNumberOfElements());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            log.error("Error al obtener reviews - ProductID: {}: {}", productId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear review", description = "Crea una nueva review para un producto")
    public ResponseEntity<ProductReviewResponse> createReview(
            @PathVariable Long productId,
            @RequestBody ProductReviewRequest request,
            HttpServletRequest httpRequest) {

        Long requestId = System.currentTimeMillis();
        Long userId = getUserIdFromToken(httpRequest);
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userId.toString());

        log.info("Creando review - ProductID: {}, User: {}", productId, userId);

        try {
            // Asegurar que el productId en el request coincida con el path
            request.setProductId(productId);

            // Validación manual después de setear el productId
            validateProductReviewRequest(request);

            ProductReviewResponse response = productReviewService.createReview(request, userId);

            log.info("Review creada exitosamente - ID: {}", response.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al crear review - ProductID: {}, User: {}: {}", productId, userId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Obtener estadísticas", description = "Obtiene estadísticas de reviews de un producto")
    public ResponseEntity<Map<String, Object>> getProductReviewStats(@PathVariable Long productId) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");

        log.info("Obteniendo estadísticas de reviews - ProductID: {}", productId);

        try {
            ProductReviewService.ProductReviewStats stats = productReviewService.getProductReviewStats(productId);

            Map<String, Object> response = Map.of(
                    "averageRating", stats.averageRating(),
                    "totalReviews", stats.totalReviews(),
                    "ratingDistribution", stats.ratingDistribution());

            log.info("Estadísticas obtenidas - ProductID: {}, Promedio: {}, Total: {}",
                    productId, stats.averageRating(), stats.totalReviews());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener estadísticas - ProductID: {}: {}", productId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/my-review")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener mi review", description = "Obtiene la review del usuario autenticado para este producto")
    public ResponseEntity<ProductReviewResponse> getMyReview(
            @PathVariable Long productId,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        Long userId = getUserIdFromToken(request);
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userId.toString());

        log.info("Obteniendo review del usuario - ProductID: {}, User: {}", productId, userId);

        try {
            // Obtener la review específica del usuario
            ProductReviewResponse response = productReviewService.getUserReviewForProduct(productId, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener review del usuario - ProductID: {}, User: {}: {}",
                    productId, userId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar review", description = "Actualiza una review existente")
    public ResponseEntity<ProductReviewResponse> updateReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewRequest request,
            HttpServletRequest httpRequest) {

        Long requestId = System.currentTimeMillis();
        Long userId = getUserIdFromToken(httpRequest);
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userId.toString());

        log.info("Actualizando review - ReviewID: {}, User: {}", reviewId, userId);

        try {
            request.setProductId(productId);
            validateProductReviewRequest(request);
            ProductReviewResponse response = productReviewService.updateReview(reviewId, request, userId);

            log.info("Review actualizada exitosamente - ID: {}", reviewId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al actualizar review - ReviewID: {}, User: {}: {}",
                    reviewId, userId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Desactivar review", description = "Desactiva una review (soft delete)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        Long userId = getUserIdFromToken(request);
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", userId.toString());

        log.info("Eliminando review - ReviewID: {}, User: {}", reviewId, userId);

        try {
            productReviewService.deleteReview(reviewId, userId);

            log.info("Review eliminada exitosamente - ID: {}", reviewId);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error al eliminar review - ReviewID: {}, User: {}: {}",
                    reviewId, userId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private void validateProductReviewRequest(ProductReviewRequest request) {
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("El ID del producto es obligatorio");
        }
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (request.getTitle().length() < 3 || request.getTitle().length() > 100) {
            throw new IllegalArgumentException("El título debe tener entre 3 y 100 caracteres");
        }
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("El comentario es obligatorio");
        }
        if (request.getComment().length() < 10 || request.getComment().length() > 1000) {
            throw new IllegalArgumentException("El comentario debe tener entre 10 y 1000 caracteres");
        }
    }
}