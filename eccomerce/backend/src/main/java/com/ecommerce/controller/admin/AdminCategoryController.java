package com.ecommerce.controller.admin;

import com.ecommerce.dto.request.CategoryRequest;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.model.entity.Category;
import com.ecommerce.service.interfaces.AuditService;
import com.ecommerce.service.interfaces.CategoryService;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Categorías", description = "Endpoints de administración de categorías")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Obtener todas las categorías", description = "Obtiene todas las categorías (incluyendo inactivas)")
    public ResponseEntity<List<Category>> getAllCategories() {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo todas las categorías");

        try {
            List<Category> categories = categoryService.getAllActiveCategories();

            log.info("Categorías obtenidas exitosamente - Cantidad: {}", categories.size());

            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            log.error("Error al obtener categorías: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(List.of());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/tree")
    @Operation(summary = "Obtener árbol de categorías", description = "Obtiene la estructura jerárquica de categorías")
    public ResponseEntity<List<Category>> getCategoryTree() {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo árbol de categorías");

        try {
            List<Category> rootCategories = categoryService.getRootCategories();

            log.info("Árbol de categorías obtenido exitosamente - Categorías raíz: {}", rootCategories.size());

            return ResponseEntity.ok(rootCategories);

        } catch (Exception e) {
            log.error("Error al obtener árbol de categorías: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(List.of());
        } finally {
            MDC.clear();
        }
    }

    @PostMapping
    @Operation(summary = "Crear categoría", description = "Crea una nueva categoría")
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin creando categoría - Nombre: '{}'", request.getName());

        try {
            Category category = categoryService.createCategory(request);

            log.info("Categoría creada exitosamente - ID: {}", category.getId());

            return ResponseEntity.ok(category);

        } catch (Exception e) {
            log.error("Error al crear categoría: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría", description = "Actualiza una categoría existente")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin actualizando categoría - ID: {}, Nombre: '{}'", id, request.getName());

        try {
            Category category = categoryService.updateCategory(id, request);

            log.info("Categoría actualizada exitosamente - ID: {}", category.getId());

            return ResponseEntity.ok(category);

        } catch (Exception e) {
            log.error("Error al actualizar categoría - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar categoría", description = "Desactiva una categoría (borrado lógico)")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin desactivando categoría - ID: {}", id);

        try {
            categoryService.deleteCategory(id);

            log.info("Categoría desactivada exitosamente - ID: {}", id);

            return ResponseEntity.ok(new MessageResponse("Categoría desactivada exitosamente"));

        } catch (Exception e) {
            log.error("Error al desactivar categoría - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Categoría no encontrada o no se puede desactivar"));
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}/eliminar")
    @Operation(summary = "Eliminar categoría permanentemente", description = "Elimina permanentemente una categoría de la base de datos")
    public ResponseEntity<MessageResponse> hardDeleteCategory(@PathVariable Long id, Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.warn("Admin eliminando categoría permanentemente - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);

            // Registrar auditoría antes de la eliminación
            log.debug("Registrando auditoría para hard delete de categoría - Admin ID: {}, Email: {}, Category ID: {}",
                    admin.getId(),
                    admin.getEmail(), id);
            AuditLog auditLog = auditService.logUserActionAndReturn(
                    admin.getId(),
                    admin.getEmail(),
                    "HARD_DELETE_CATEGORY",
                    "CATEGORY",
                    id.toString(),
                    "Eliminación permanente de la categoría ID: " + id,
                    true);
            log.debug("Auditoría registrada exitosamente para hard delete de categoría");

            categoryService.hardDeleteCategory(id);

            // Enviar notificación a todos los administradores
            try {
                emailService.sendHardDeleteNotificationToAdmins(
                        admin.getEmail(),
                        "CATEGORY",
                        id.toString(),
                        "El administrador " + admin.getEmail() + " eliminó permanentemente la categoría ID: " + id,
                        auditLog);
                log.info("Notificación de hard delete de categoría enviada a administradores");
            } catch (Exception e) {
                log.error("Error enviando notificación de hard delete de categoría: {}", e.getMessage());
                // No fallar la operación por error en notificación
            }

            log.warn("Categoría eliminada permanentemente - ID: {}", id);

            return ResponseEntity.ok(new MessageResponse("Categoría eliminada permanentemente"));

        } catch (Exception e) {
            log.error("Error al eliminar categoría permanentemente - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Categoría no encontrada o no se puede eliminar"));
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}/order")
    @Operation(summary = "Actualizar orden", description = "Actualiza el orden de una categoría")
    public ResponseEntity<Category> updateCategoryOrder(@PathVariable Long id, @RequestParam Integer sortOrder) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin actualizando orden de categoría - ID: {}, Nuevo orden: {}", id, sortOrder);

        try {
            Category category = categoryService.getCategoryById(id);
            CategoryRequest updateRequest = CategoryRequest.builder()
                    .name(category.getName())
                    .description(category.getDescription())
                    .parentId(category.getParent() != null ? category.getParent().getId() : null)
                    .sortOrder(sortOrder)
                    .active(category.getActive())
                    .build();

            Category updatedCategory = categoryService.updateCategory(id, updateRequest);

            log.info("Orden de categoría actualizado exitosamente - ID: {}", id);

            return ResponseEntity.ok(updatedCategory);

        } catch (Exception e) {
            log.error("Error al actualizar orden de categoría - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Reactivar categoría", description = "Reactiva una categoría previamente desactivada")
    public ResponseEntity<Void> restoreCategory(@PathVariable Long id) {
        log.info("Admin restoring category - ID: {}", id);

        try {
            categoryService.restoreCategory(id);
            log.info("Category restored successfully - ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error restoring category {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}