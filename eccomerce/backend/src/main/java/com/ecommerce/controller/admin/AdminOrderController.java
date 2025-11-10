package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.dto.response.OrderStatisticsResponse;
import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.service.interfaces.AuditService;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.OrderService;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Órdenes", description = "Endpoints de administración de órdenes")
public class AdminOrderController {

    private final OrderService orderService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Obtener todas las órdenes", description = "Obtiene todas las órdenes del sistema")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo todas las órdenes");

        try {
            List<OrderResponse> orders = orderService.getAllOrders();

            log.info("Órdenes obtenidas exitosamente - Cantidad: {}", orders.size());

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error al obtener órdenes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(List.of());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener orden por ID", description = "Obtiene los detalles de una orden específica")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo orden por ID: {}", id);

        try {
            // Para admin, podemos obtener cualquier orden
            OrderResponse order = orderService.getOrderById(id, "admin");

            log.info("Orden obtenida exitosamente - ID: {}", id);

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error al obtener orden - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de orden", description = "Actualiza el estado de una orden")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin actualizando estado de orden - ID: {}, Nuevo estado: {}", id, status);

        try {
            OrderResponse order = orderService.updateOrderStatus(id, status);

            log.info("Estado de orden actualizado exitosamente - ID: {}, Estado: {}", id, status);

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error al actualizar estado de orden - ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Obtener órdenes por estado", description = "Obtiene órdenes filtradas por estado")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable String status) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo órdenes por estado: {}", status);

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<OrderResponse> orders = orderService.getOrdersByStatusAsResponse(orderStatus);

            log.info("Órdenes por estado obtenidas - Estado: {}, Cantidad: {}", status, orders.size());

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error al obtener órdenes por estado: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(List.of());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/date-range")
    @Operation(summary = "Obtener órdenes por rango de fechas", description = "Obtiene órdenes dentro de un rango de fechas")
    public ResponseEntity<List<OrderResponse>> getOrdersByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo órdenes por rango de fechas - Inicio: {}, Fin: {}", startDate, endDate);

        try {
            List<OrderResponse> orders = orderService.getOrdersByDateRangeAsResponse(startDate, endDate);

            log.info("Órdenes por rango de fechas obtenidas - Cantidad: {}", orders.size());

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error al obtener órdenes por rango de fechas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(List.of());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Obtener estadísticas de órdenes", description = "Obtiene estadísticas generales de órdenes")
    public ResponseEntity<OrderStatisticsResponse> getOrderStatistics() {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo estadísticas de órdenes");

        try {
            OrderStatisticsResponse statistics = orderService.getOrderStatistics();

            log.info("Estadísticas de órdenes obtenidas exitosamente - Total órdenes: {}", statistics.getTotalOrders());

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error al obtener estadísticas de órdenes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/revenue")
    @Operation(summary = "Obtener ingresos", description = "Obtiene los ingresos totales en un período")
    public ResponseEntity<Double> getRevenue(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin obteniendo ingresos - Inicio: {}, Fin: {}", startDate, endDate);

        try {
            Double revenue = orderService.getTotalRevenue(startDate, endDate);

            log.info("Ingresos obtenidos exitosamente: {}", revenue);

            return ResponseEntity.ok(revenue);

        } catch (Exception e) {
            log.error("Error al obtener ingresos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(0.0);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar orden", description = "Cancela una orden (borrado lógico)")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Admin desactivando orden - ID: {}", id);

        try {
            orderService.deleteOrder(id);
            log.info("Orden desactivada (cancelada) exitosamente - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al desactivar orden {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/eliminar")
    @Operation(summary = "Eliminar orden permanentemente", description = "Elimina permanentemente una orden de la base de datos")
    public ResponseEntity<Void> hardDeleteOrder(@PathVariable Long id, Authentication authentication) {
        log.warn("Admin eliminando orden permanentemente - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);

            // Registrar auditoría antes de la eliminación
            log.debug("Registrando auditoría para hard delete de orden - Admin ID: {}, Email: {}, Order ID: {}",
                    admin.getId(),
                    admin.getEmail(), id);
            AuditLog auditLog = auditService.logUserActionAndReturn(
                    admin.getId(),
                    admin.getEmail(),
                    "HARD_DELETE_ORDER",
                    "ORDER",
                    id.toString(),
                    "Eliminación permanente de la orden ID: " + id,
                    true);
            log.debug("Auditoría registrada exitosamente para hard delete de orden");

            orderService.hardDeleteOrder(id);

            // Enviar notificación a todos los administradores
            try {
                emailService.sendHardDeleteNotificationToAdmins(
                        admin.getEmail(),
                        "ORDER",
                        id.toString(),
                        "El administrador " + admin.getEmail() + " eliminó permanentemente la orden ID: " + id,
                        auditLog);
                log.info("Notificación de hard delete de orden enviada a administradores");
            } catch (Exception e) {
                log.error("Error enviando notificación de hard delete de orden: {}", e.getMessage());
                // No fallar la operación por error en notificación
            }

            log.warn("Orden eliminada permanentemente - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar orden permanentemente {}: {}", id, e.getMessage());

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                com.ecommerce.model.entity.User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_ORDER",
                        "ORDER",
                        id.toString(),
                        "Intento fallido de eliminación permanente de la orden ID: " + id,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Reactivar orden", description = "Reactiva una orden previamente cancelada")
    public ResponseEntity<Void> restoreOrder(@PathVariable Long id) {
        log.info("Admin restoring order - ID: {}", id);

        try {
            orderService.restoreOrder(id);
            log.info("Order restored successfully - ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error restoring order {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}