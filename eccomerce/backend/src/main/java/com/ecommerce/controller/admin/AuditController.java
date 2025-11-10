package com.ecommerce.controller.admin;

import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.service.interfaces.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoría", description = "Endpoints de auditoría y logging (solo administradores)")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Obtener logs de auditoría", description = "Obtiene una lista paginada de todos los logs de auditoría con filtros avanzados y ordenamiento")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin-audit");

        // Validar y configurar ordenamiento
        String sortField = getValidSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        log.info(
                "Admin accediendo a logs de auditoría - Página: {}, Tamaño: {}, Ordenar por: {} {}, Filtros: userEmail={}, action={}, entityType={}, entityId={}, success={}, startDate={}, endDate={}",
                page, size, sortField, direction, userEmail, action, entityType, entityId, success, startDate, endDate);

        try {
            Page<AuditLog> auditLogs = auditService.getAllAuditLogs(pageable);

            log.info("Logs de auditoría obtenidos exitosamente - Total: {}", auditLogs.getTotalElements());

            return ResponseEntity.ok(auditLogs);

        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private String getValidSortField(String sortBy) {
        if (sortBy == null)
            return "timestamp";

        // Campos válidos para ordenamiento
        switch (sortBy.toLowerCase()) {
            case "timestamp":
            case "action":
            case "entitytype":
            case "entityid":
            case "userid":
            case "useremail":
            case "success":
            case "executiontimems":
                return sortBy.equalsIgnoreCase("entitytype") ? "entityType"
                        : sortBy.equalsIgnoreCase("entityid") ? "entityId"
                                : sortBy.equalsIgnoreCase("userid") ? "userId"
                                        : sortBy.equalsIgnoreCase("useremail") ? "userEmail"
                                                : sortBy.equalsIgnoreCase("executiontimems") ? "executionTimeMs"
                                                        : sortBy;
            default:
                return "timestamp";
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener logs por usuario", description = "Obtiene los logs de auditoría de un usuario específico")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(@PathVariable Long userId,
            @PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin-audit");

        log.info("Admin obteniendo logs de auditoría para usuario ID: {}", userId);

        try {
            Page<AuditLog> auditLogs = auditService.getAuditLogsByUser(userId, pageable);

            log.info("Logs de auditoría de usuario obtenidos - UserID: {}, Cantidad: {}", userId,
                    auditLogs.getNumberOfElements());

            return ResponseEntity.ok(auditLogs);

        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría de usuario - UserID: {}, Error: {}", userId, e.getMessage(),
                    e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Obtener logs por entidad", description = "Obtiene los logs de auditoría relacionados con una entidad específica")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin-audit");

        log.info("Admin obteniendo logs de auditoría para entidad - Tipo: {}, ID: {}", entityType, entityId);

        try {
            Page<AuditLog> auditLogs = auditService.getAuditLogsByEntity(entityType, entityId, pageable);

            log.info("Logs de auditoría de entidad obtenidos - Tipo: {}, ID: {}, Cantidad: {}", entityType, entityId,
                    auditLogs.getNumberOfElements());

            return ResponseEntity.ok(auditLogs);

        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría de entidad - Tipo: {}, ID: {}, Error: {}", entityType,
                    entityId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/errors")
    @Operation(summary = "Obtener logs de error", description = "Obtiene todos los logs de auditoría que registran errores")
    public ResponseEntity<Page<AuditLog>> getErrorLogs(
            @PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin-audit");

        log.info("Admin obteniendo logs de auditoría de errores");

        try {
            Page<AuditLog> errorLogs = auditService.getErrorLogs(pageable);

            log.info("Logs de auditoría de errores obtenidos - Cantidad: {}", errorLogs.getNumberOfElements());

            return ResponseEntity.ok(errorLogs);

        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría de errores: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/date-range")
    @Operation(summary = "Obtener logs por rango de fechas", description = "Obtiene logs de auditoría dentro de un rango de fechas")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @PageableDefault(sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin-audit");

        log.info("Admin obteniendo logs de auditoría por rango de fechas - Inicio: {}, Fin: {}", startDate, endDate);

        try {
            Page<AuditLog> auditLogs = auditService.getAuditLogsByDateRange(startDate, endDate, pageable);

            log.info("Logs de auditoría por rango de fechas obtenidos - Inicio: {}, Fin: {}, Cantidad: {}", startDate,
                    endDate, auditLogs.getNumberOfElements());

            return ResponseEntity.ok(auditLogs);

        } catch (Exception e) {
            log.error("Error al obtener logs de auditoría por rango de fechas - Inicio: {}, Fin: {}, Error: {}",
                    startDate, endDate, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Obtener estadísticas de auditoría", description = "Obtiene estadísticas generales del sistema de auditoría")
    public ResponseEntity<Map<String, Object>> getAuditStatistics() {

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("userId", "admin-audit");

        log.info("Admin obteniendo estadísticas de auditoría");

        try {
            Map<String, Object> statistics = auditService.getAuditStatistics();

            log.info("Estadísticas de auditoría obtenidas exitosamente");

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error al obtener estadísticas de auditoría: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/action-statistics")
    @Operation(summary = "Obtener estadísticas por acción", description = "Obtiene estadísticas agrupadas por tipo de acción")
    public ResponseEntity<List<Object[]>> getActionStatistics() {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin-audit");

        log.info("Admin obteniendo estadísticas por acción");

        try {
            List<Object[]> statistics = auditService.getActionStatistics();

            log.info("Estadísticas por acción obtenidas - Cantidad de acciones: {}", statistics.size());

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Error al obtener estadísticas por acción: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/test-data")
    @Operation(summary = "Crear datos de prueba", description = "Crea algunos logs de auditoría de prueba para testing")
    public ResponseEntity<Map<String, String>> createTestData() {

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("userId", "admin-test");

        log.info("Admin creando datos de prueba de auditoría");

        try {
            // Crear algunos logs de prueba
            auditService.logUserAction(1L, "admin@test.com", "LOGIN", "User", "1", "Inicio de sesión exitoso", true);
            auditService.logUserAction(1L, "admin@test.com", "PRODUCT_CREATE", "Product", "1", "Producto creado", true);
            auditService.logUserAction(1L, "admin@test.com", "USER_UPDATE", "User", "2", "Usuario actualizado", true);
            auditService.logUserAction(1L, "admin@test.com", "ORDER_CREATE", "Order", "1", "Pedido creado", true);
            auditService.logUserAction(1L, "admin@test.com", "PRODUCT_DELETE", "Product", "2", "Producto eliminado",
                    false, "Producto no encontrado");

            auditService.logEvent("SYSTEM_BACKUP", "System", "backup-1", "Respaldo completado", true);
            auditService.logHttpRequest("GET", "/api/products", "192.168.1.1", "Mozilla/5.0", 150L, true);

            log.info("Datos de prueba de auditoría creados exitosamente");

            return ResponseEntity.ok(Map.of("message", "Datos de prueba creados exitosamente"));

        } catch (Exception e) {
            log.error("Error al crear datos de prueba: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/count")
    @Operation(summary = "Contar logs de auditoría", description = "Devuelve el número total de logs de auditoría en la base de datos")
    public ResponseEntity<Map<String, Long>> getAuditCount() {

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("userId", "admin-count");

        log.info("Admin consultando cantidad de logs de auditoría");

        try {
            long totalCount = auditService.getAllAuditLogs(Pageable.unpaged()).getTotalElements();

            log.info("Total de logs de auditoría: {}", totalCount);

            return ResponseEntity.ok(Map.of("total", totalCount));

        } catch (Exception e) {
            log.error("Error al contar logs de auditoría: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
