package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.ProductReviewResponse;
import com.ecommerce.dto.response.ProductReviewStatsResponse;
import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.service.interfaces.AuditService;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.ProductReviewService;
import com.ecommerce.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Reviews", description = "Endpoints de administración de reviews de productos")
public class AdminProductReviewController {

    private final ProductReviewService productReviewService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Obtener todas las reviews", description = "Obtiene todas las reviews para moderar")
    public ResponseEntity<Page<ProductReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo todas las reviews - Página: {}, Tamaño: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            // Obtener todas las reviews (pendientes y aprobadas) para moderación
            Page<ProductReviewResponse> reviews = productReviewService.getPendingReviews(pageable);

            log.info("Reviews obtenidas - Cantidad: {}", reviews.getNumberOfElements());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            log.error("Error al obtener reviews: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Page.empty());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/pending")
    @Operation(summary = "Obtener reviews pendientes", description = "Obtiene todas las reviews pendientes de aprobación")
    public ResponseEntity<Page<ProductReviewResponse>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo reviews pendientes - Página: {}, Tamaño: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductReviewResponse> reviews = productReviewService.getPendingReviews(pageable);

            log.info("Reviews pendientes obtenidas - Cantidad: {}", reviews.getNumberOfElements());

            return ResponseEntity.ok(reviews);

        } catch (Exception e) {
            log.error("Error al obtener reviews pendientes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Page.empty());
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{reviewId}/approve")
    @Operation(summary = "Aprobar review", description = "Aprueba una review")
    public ResponseEntity<ProductReviewResponse> approveReview(@PathVariable Long reviewId) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin aprobando review - ReviewID: {}", reviewId);

        try {
            ProductReviewResponse response = productReviewService.approveReview(reviewId);

            log.info("Review aprobada exitosamente - ID: {}", reviewId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al aprobar review - ReviewID: {}: {}", reviewId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{reviewId}/reject")
    @Operation(summary = "Rechazar review", description = "Rechaza y elimina una review")
    public ResponseEntity<Void> rejectReview(@PathVariable Long reviewId) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin rechazando review - ReviewID: {}", reviewId);

        try {
            productReviewService.rejectReview(reviewId);

            log.info("Review rechazada exitosamente - ID: {}", reviewId);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error al rechazar review - ReviewID: {}: {}", reviewId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Obtener review por ID", description = "Obtiene los detalles de una review específica")
    public ResponseEntity<ProductReviewResponse> getReviewById(@PathVariable Long reviewId) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo review por ID: {}", reviewId);

        try {
            ProductReviewResponse response = productReviewService.getReviewById(reviewId);

            log.info("Review obtenida exitosamente - ID: {}", reviewId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener review - ReviewID: {}: {}", reviewId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/stats/{productId}")
    @Operation(summary = "Obtener estadísticas de producto", description = "Obtiene estadísticas de reviews de un producto")
    public ResponseEntity<ProductReviewStatsResponse> getProductReviewStats(@PathVariable Long productId) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo estadísticas de reviews - ProductID: {}", productId);

        try {
            ProductReviewService.ProductReviewStats stats = productReviewService.getProductReviewStats(productId);

            ProductReviewStatsResponse response = new ProductReviewStatsResponse(
                    productId,
                    stats.averageRating(),
                    stats.totalReviews(),
                    stats.ratingDistribution());

            log.info("Estadísticas obtenidas - ProductID: {}, Promedio: {}, Total: {}",
                    productId, stats.averageRating(), stats.totalReviews());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener estadísticas - ProductID: {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ocultar review con razón", description = "Oculta una review indicando la razón (borrado lógico con auditoría)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @RequestBody com.ecommerce.dto.HideReviewRequest request,
            Authentication authentication) {
        log.info("Admin ocultando review - ID: {}, Razón: {}", id, request.getReason());

        try {
            // Obtener información del admin autenticado
            String adminEmail = authentication.getName();
            com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);

            productReviewService.deleteReviewWithReason(id, admin.getId(), request.getReason());
            log.info("Review ocultada exitosamente - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al ocultar review {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/eliminar")
    @Operation(summary = "Eliminar review permanentemente", description = "Elimina permanentemente una review de la base de datos")
    public ResponseEntity<Void> hardDeleteReview(@PathVariable Long id, Authentication authentication) {
        log.warn("Admin eliminando review permanentemente - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);

            // Registrar auditoría antes de la eliminación
            log.debug("Registrando auditoría para hard delete de review - Admin ID: {}, Email: {}, Review ID: {}",
                    admin.getId(),
                    admin.getEmail(), id);
            AuditLog auditLog = auditService.logUserActionAndReturn(
                    admin.getId(),
                    admin.getEmail(),
                    "HARD_DELETE_REVIEW",
                    "PRODUCT_REVIEW",
                    id.toString(),
                    "Eliminación permanente de la review ID: " + id,
                    true);
            log.debug("Auditoría registrada exitosamente para hard delete de review");

            productReviewService.hardDeleteReview(id);

            // Enviar notificación a todos los administradores
            try {
                emailService.sendHardDeleteNotificationToAdmins(
                        admin.getEmail(),
                        "PRODUCT_REVIEW",
                        id.toString(),
                        "El administrador " + admin.getEmail() + " eliminó permanentemente la review ID: " + id,
                        auditLog);
                log.info("Notificación de hard delete de review enviada a administradores");
            } catch (Exception e) {
                log.error("Error enviando notificación de hard delete de review: {}", e.getMessage());
                // No fallar la operación por error en notificación
            }

            log.warn("Review eliminada permanentemente - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar review permanentemente {}: {}", id, e.getMessage());

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_REVIEW",
                        "PRODUCT_REVIEW",
                        id.toString(),
                        "Intento fallido de eliminación permanente de la review ID: " + id,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Reactivar review", description = "Reactiva una review previamente eliminada")
    public ResponseEntity<Void> restoreReview(@PathVariable Long id) {
        log.info("Admin restoring review - ID: {}", id);

        try {
            productReviewService.restoreReview(id);
            log.info("Review restored successfully - ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error restoring review {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}