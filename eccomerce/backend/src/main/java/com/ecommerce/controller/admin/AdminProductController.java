package com.ecommerce.controller.admin;

import com.ecommerce.dto.request.HardDeleteRequest;
import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.model.entity.Product;
import com.ecommerce.model.enums.ProductStatus;
import com.ecommerce.service.interfaces.AuditService;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.ProductService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Productos", description = "Endpoints de administración de productos")
public class AdminProductController {

    private final ProductService productService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Obtener todos los productos", description = "Obtiene una lista paginada de todos los productos (incluyendo inactivos)")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo todos los productos - Página: {}, Tamaño: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            // Usar getAllProductsForAdmin en lugar de getAllActiveProducts para mostrar
            // todos los productos
            Page<Product> productPage = productService.getAllProductsForAdmin(pageable);
            List<Product> products = productPage.getContent();

            List<ProductResponse> productResponses = products.stream()
                    .map(this::convertToProductResponse)
                    .collect(java.util.stream.Collectors.toList());

            Page<ProductResponse> responsePage = new PageImpl<>(productResponses, pageable,
                    productPage.getTotalElements());

            log.info("Productos obtenidos exitosamente - Total: {}", productPage.getTotalElements());

            return ResponseEntity.ok(responsePage);

        } catch (Exception e) {
            log.error("Error al obtener productos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Page.empty());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos", description = "Busca productos por nombre o descripción")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin buscando productos - Query: {}, Página: {}, Tamaño: {}", query, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> productPage = productService.searchProductsForAdmin(query, pageable);
            List<Product> products = productPage.getContent();

            List<ProductResponse> productResponses = products.stream()
                    .map(this::convertToProductResponse)
                    .collect(java.util.stream.Collectors.toList());

            Page<ProductResponse> responsePage = new PageImpl<>(productResponses, pageable,
                    productPage.getTotalElements());

            log.info("Productos encontrados exitosamente - Total: {}", productPage.getTotalElements());

            return ResponseEntity.ok(responsePage);

        } catch (Exception e) {
            log.error("Error al buscar productos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Page.empty());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Obtener productos con stock bajo", description = "Obtiene productos con stock por debajo del umbral")
    public ResponseEntity<List<Product>> getLowStockProducts(@RequestParam(defaultValue = "10") Integer threshold) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo productos con stock bajo - Umbral: {}", threshold);

        try {
            List<Product> products = productService.getLowStockProducts(threshold);

            log.info("Productos con stock bajo obtenidos - Cantidad: {}", products.size());

            return ResponseEntity.ok(products);

        } catch (Exception e) {
            log.error("Error al obtener productos con stock bajo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(List.of());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Obtiene un producto específico por su ID (solo para admin)")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo producto - ID: {}", id);

        try {
            Product product = productService.getProductById(id);
            ProductResponse response = convertToProductResponse(product);

            log.info("Producto obtenido - ID: {}, Nombre: '{}'", product.getId(), product.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener producto: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin creando producto - Nombre: '{}'", request.getName());

        try {
            Product product = productService.createProduct(request);
            ProductResponse response = convertToProductResponse(product);

            log.info("Producto creado exitosamente - ID: {}", product.getId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear producto: {}", e.getMessage());
            return (ResponseEntity<?>) ResponseEntity.badRequest()
                    .body(new MessageResponse("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al crear producto: {}", e.getMessage(), e);
            return (ResponseEntity<?>) ResponseEntity.status(500)
                    .body(new MessageResponse("Error interno del servidor: " + e.getMessage()));
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "Actualiza un producto existente")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin actualizando producto - ID: {}, Nombre: '{}'", id, request.getName());
        log.debug("Datos recibidos - Price: {}, Stock: {}, Active: {}, Featured: {}",
                request.getPrice(), request.getStockQuantity(), request.getActive(), request.getFeatured());

        try {
            Product product = productService.updateProduct(id, request);
            ProductResponse response = convertToProductResponse(product);

            log.info("Producto actualizado exitosamente - ID: {}", product.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar producto - ID: {}: {}", id, e.getMessage());
            return (ResponseEntity<?>) ResponseEntity.badRequest()
                    .body(new MessageResponse("Error de validación: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al actualizar producto - ID: {}: {}", id, e.getMessage(), e);
            return (ResponseEntity<?>) ResponseEntity.status(500)
                    .body(new MessageResponse("Error interno del servidor: " + e.getMessage()));
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar producto", description = "Desactiva un producto (borrado lógico)")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable Long id) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin desactivando producto - ID: {}", id);

        try {
            productService.deleteProduct(id);

            log.info("Producto desactivado exitosamente - ID: {}", id);

            return ResponseEntity.ok(new MessageResponse("Producto desactivado exitosamente"));

        } catch (Exception e) {
            log.error("Error al desactivar producto - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Producto no encontrado o no se puede desactivar"));
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}/eliminar")
    @Operation(summary = "Eliminar producto permanentemente", description = "Elimina permanentemente un producto de la base de datos (requiere contraseña del admin)")
    public ResponseEntity<MessageResponse> hardDeleteProduct(@PathVariable Long id,
            @Valid @RequestBody HardDeleteRequest request, Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.warn("Admin eliminando producto permanentemente - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);

            // Verificar que la contraseña proporcionada sea correcta
            if (!passwordEncoder.matches(request.getConfirmationPassword(), admin.getPassword())) {
                log.warn("Contraseña incorrecta para hard delete de producto - Admin: {}, Product ID: {}", adminEmail,
                        id);

                // Registrar auditoría del intento fallido
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_PRODUCT",
                        "PRODUCT",
                        id.toString(),
                        "Intento fallido de eliminación permanente - contraseña incorrecta",
                        false);

                return ResponseEntity.status(401)
                        .body(new MessageResponse("Contraseña incorrecta"));
            }

            // Registrar auditoría antes de la eliminación
            log.debug("Registrando auditoría para hard delete de producto - Admin ID: {}, Email: {}, Product ID: {}",
                    admin.getId(),
                    admin.getEmail(), id);
            AuditLog auditLog = auditService.logUserActionAndReturn(
                    admin.getId(),
                    admin.getEmail(),
                    "HARD_DELETE_PRODUCT",
                    "PRODUCT",
                    id.toString(),
                    "Eliminación permanente del producto ID: " + id,
                    true);
            log.debug("Auditoría registrada exitosamente para hard delete de producto");

            productService.hardDeleteProduct(id);

            // Enviar notificación a todos los administradores
            try {
                emailService.sendHardDeleteNotificationToAdmins(
                        admin.getEmail(),
                        "PRODUCT",
                        id.toString(),
                        "El administrador " + admin.getEmail() + " eliminó permanentemente el producto ID: " + id,
                        auditLog);
                log.info("Notificación de hard delete de producto enviada a administradores");
            } catch (Exception e) {
                log.error("Error enviando notificación de hard delete de producto: {}", e.getMessage());
                // No fallar la operación por error en notificación
            }

            log.warn("Producto eliminado permanentemente - ID: {}", id);

            return ResponseEntity.ok(new MessageResponse("Producto eliminado permanentemente"));

        } catch (Exception e) {
            log.error("Error al eliminar producto permanentemente - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Producto no encontrado o no se puede eliminar"));
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}/stock")
    @Operation(summary = "Actualizar stock", description = "Actualiza el stock de un producto")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestParam Integer stockQuantity) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin actualizando stock - ID: {}, Nuevo stock: {}", id, stockQuantity);

        try {
            Product product = productService.getProductById(id);
            ProductRequest updateRequest = ProductRequest.builder()
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .stockQuantity(stockQuantity)
                    .sku(product.getSku())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .active(product.getStatus() == ProductStatus.ACTIVE)
                    .featured(product.getFeatured())
                    .build();

            Product updatedProduct = productService.updateProduct(id, updateRequest);

            log.info("Stock actualizado exitosamente - ID: {}, Nuevo stock: {}", id, stockQuantity);

            return ResponseEntity.ok(updatedProduct);

        } catch (Exception e) {
            log.error("Error al actualizar stock - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Reactivar producto", description = "Reactiva un producto previamente desactivado")
    public ResponseEntity<Void> restoreProduct(@PathVariable Long id) {
        log.info("Admin restoring product - ID: {}", id);

        try {
            productService.restoreProduct(id);
            log.info("Product restored successfully - ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error restoring product {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private ProductResponse convertToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty()
                        ? product.getImages().get(0).getImageUrl()
                        : null)
                .category(product.getCategory() != null ? convertToCategoryResponse(product.getCategory()) : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .active(product.getActive())
                .featured(product.getFeatured())
                .build();
    }

    private com.ecommerce.dto.response.CategoryResponse convertToCategoryResponse(
            com.ecommerce.model.entity.Category category) {
        return com.ecommerce.dto.response.CategoryResponse.builder()
                .id(category.getId().toString())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}