package com.ecommerce.service.interfaces;

import com.ecommerce.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AuditService {

        // Registrar eventos de auditoría
        void logEvent(String action, String entityType, String entityId, String details, boolean success);

        void logEvent(String action, String entityType, String entityId, String details, boolean success,
                        String errorMessage);

        void logUserAction(Long userId, String userEmail, String action, String entityType, String entityId,
                        String details,
                        boolean success);

        void logUserAction(Long userId, String userEmail, String action, String entityType, String entityId,
                        String details,
                        boolean success, String errorMessage);

        AuditLog logUserActionAndReturn(Long userId, String userEmail, String action, String entityType,
                        String entityId,
                        String details, boolean success);

        AuditLog logUserActionAndReturn(Long userId, String userEmail, String action, String entityType,
                        String entityId,
                        String details, boolean success, String errorMessage);

        void logHttpRequest(String method, String url, String ipAddress, String userAgent, Long executionTimeMs,
                        boolean success);

        void logHttpRequest(String method, String url, String ipAddress, String userAgent, Long executionTimeMs,
                        boolean success, String errorMessage);

        void logHttpRequest(String method, String url, String ipAddress, String userAgent, Long executionTimeMs,
                        boolean success, String errorMessage, Long userId, String userEmail);

        // Consultas de auditoría
        Page<AuditLog> getAllAuditLogs(Pageable pageable);

        Page<AuditLog> getFilteredAuditLogs(Pageable pageable, String userEmail, String action, String entityType,
                        String entityId, Boolean success, LocalDateTime startDate, LocalDateTime endDate);

        Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable);

        Page<AuditLog> getAuditLogsByUserEmail(String userEmail, Pageable pageable);

        Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable);

        Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable);

        Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

        Page<AuditLog> getErrorLogs(Pageable pageable);

        Page<AuditLog> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable);

        // Estadísticas
        Map<String, Object> getAuditStatistics();

        Map<String, Object> getAuditStatisticsSince(LocalDateTime since);

        List<Object[]> getActionStatistics();

        List<Object[]> getActionStatisticsSince(LocalDateTime since);

        // Limpieza
        void cleanupOldLogs(LocalDateTime beforeDate);
}
