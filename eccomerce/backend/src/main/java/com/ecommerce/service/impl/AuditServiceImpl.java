package com.ecommerce.service.impl;

import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.repository.AuditLogRepository;
import com.ecommerce.service.interfaces.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String action, String entityType, String entityId, String details, boolean success) {
        logEvent(action, entityType, entityId, details, success, null);
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String action, String entityType, String entityId, String details, boolean success,
            String errorMessage) {
        try {
            // Truncar strings largos para evitar errores de base de datos
            String truncatedDetails = details != null && details.length() > 1000 ? details.substring(0, 997) + "..."
                    : details;
            String truncatedErrorMessage = errorMessage != null && errorMessage.length() > 5000
                    ? errorMessage.substring(0, 4997) + "..."
                    : errorMessage;

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(truncatedDetails)
                    .success(success)
                    .errorMessage(truncatedErrorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

            if (success) {
                // log.info("Evento de auditoría registrado: {} en {} {}", action, entityType,
                // entityId != null ? entityId : "");
            } else {
                log.warn("Error de auditoría registrado: {} en {} {} - Error: {}", action, entityType,
                        entityId != null ? entityId : "", errorMessage);
            }
        } catch (Exception e) {
            log.error("Error al registrar evento de auditoría: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserAction(Long userId, String userEmail, String action, String entityType, String entityId,
            String details, boolean success) {
        logUserActionAndReturn(userId, userEmail, action, entityType, entityId, details, success, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserAction(Long userId, String userEmail, String action, String entityType, String entityId,
            String details, boolean success, String errorMessage) {
        logUserActionAndReturn(userId, userEmail, action, entityType, entityId, details, success, errorMessage);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logUserActionAndReturn(Long userId, String userEmail, String action, String entityType,
            String entityId, String details, boolean success) {
        return logUserActionAndReturn(userId, userEmail, action, entityType, entityId, details, success, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logUserActionAndReturn(Long userId, String userEmail, String action, String entityType,
            String entityId, String details, boolean success, String errorMessage) {
        try {
            log.debug("Intentando guardar auditoría: userId={}, userEmail={}, action={}, entityType={}, entityId={}",
                    userId, userEmail, action, entityType, entityId);

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .success(success)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            AuditLog savedLog = auditLogRepository.save(auditLog);
            log.debug("Auditoría guardada exitosamente con ID: {}", savedLog.getId());

            if (success) {
                // log.info("Evento de auditoría de usuario registrado: Usuario {} realizó {} en
                // {} {}", userEmail, action,
                // entityType, entityId != null ? entityId : "");
            } else {
                log.warn("Error de auditoría de usuario registrado: Usuario {} falló {} en {} {} - Error: {}",
                        userEmail, action, entityType, entityId != null ? entityId : "", errorMessage);
            }

            return savedLog;
        } catch (Exception e) {
            log.error("Error al registrar evento de auditoría de usuario: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Async
    @Transactional
    public void logHttpRequest(String method, String url, String ipAddress, String userAgent, Long executionTimeMs,
            boolean success) {
        logHttpRequest(method, url, ipAddress, userAgent, executionTimeMs, success, null, null, null);
    }

    @Override
    @Async
    @Transactional
    public void logHttpRequest(String method, String url, String ipAddress, String userAgent, Long executionTimeMs,
            boolean success, String errorMessage) {
        logHttpRequest(method, url, ipAddress, userAgent, executionTimeMs, success, errorMessage, null, null);
    }

    @Override
    @Async
    @Transactional
    public void logHttpRequest(String method, String url, String ipAddress, String userAgent, Long executionTimeMs,
            boolean success, String errorMessage, Long userId, String userEmail) {
        try {
            // Truncar strings largos
            String truncatedUrl = url != null && url.length() > 1000 ? url.substring(0, 997) + "..." : url;
            String truncatedUserAgent = userAgent != null && userAgent.length() > 500
                    ? userAgent.substring(0, 497) + "..."
                    : userAgent;
            String truncatedErrorMessage = errorMessage != null && errorMessage.length() > 5000
                    ? errorMessage.substring(0, 4997) + "..."
                    : errorMessage;

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action("HTTP_REQUEST")
                    .entityType("HTTP")
                    .entityId(method + " " + truncatedUrl)
                    .details("HTTP Request: " + method + " " + truncatedUrl)
                    .ipAddress(ipAddress)
                    .userAgent(truncatedUserAgent)
                    .executionTimeMs(executionTimeMs)
                    .requestUrl(truncatedUrl)
                    .success(success)
                    .errorMessage(truncatedErrorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

            if (success) {
                // log.info("Petición HTTP registrada: {} {} desde {} ({}ms) - Usuario: {}",
                // method, url, ipAddress, executionTimeMs, userEmail != null ? userEmail :
                // "anónimo");
            } else {
                log.warn("Error HTTP registrado: {} {} desde {} - Error: {} - Usuario: {}",
                        method, url, ipAddress, errorMessage, userEmail != null ? userEmail : "anónimo");
            }
        } catch (Exception e) {
            log.error("Error al registrar petición HTTP: {}", e.getMessage(), e);
        }
    }

    @Override
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Override
    public Page<AuditLog> getFilteredAuditLogs(Pageable pageable, String userEmail, String action, String entityType,
            String entityId, Boolean success, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findFilteredAuditLogs(
                userEmail, action, entityType, entityId, success, startDate, endDate, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByUserEmail(String userEmail, Pageable pageable) {
        return auditLogRepository.findByUserEmailOrderByTimestampDesc(userEmail, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }

    @Override
    public Page<AuditLog> getErrorLogs(Pageable pageable) {
        return auditLogRepository.findErrorLogs(pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId, pageable);
    }

    @Override
    public Map<String, Object> getAuditStatistics() {
        return getAuditStatisticsSince(LocalDateTime.now().minusDays(30));
    }

    @Override
    public Map<String, Object> getAuditStatisticsSince(LocalDateTime since) {
        Map<String, Object> stats = new HashMap<>();

        long totalEvents = auditLogRepository.count();
        long totalErrors = auditLogRepository.countErrorsSince(since);
        long totalLogins = auditLogRepository.countByActionSince("LOGIN", since);
        long totalFailedLogins = auditLogRepository.countByActionSince("LOGIN_FAILED", since);

        stats.put("totalEvents", totalEvents);
        stats.put("totalErrors", totalErrors);
        stats.put("totalLogins", totalLogins);
        stats.put("totalFailedLogins", totalFailedLogins);
        stats.put("errorRate", totalEvents > 0 ? (double) totalErrors / totalEvents * 100 : 0);
        stats.put("loginSuccessRate",
                (totalLogins + totalFailedLogins) > 0 ? (double) totalLogins / (totalLogins + totalFailedLogins) * 100
                        : 0);

        return stats;
    }

    @Override
    public List<Object[]> getActionStatistics() {
        return auditLogRepository.getActionStatsSince(LocalDateTime.now().minusDays(30));
    }

    @Override
    public List<Object[]> getActionStatisticsSince(LocalDateTime since) {
        return auditLogRepository.getActionStatsSince(since);
    }

    @Override
    @Transactional
    public void cleanupOldLogs(LocalDateTime beforeDate) {
        try {
            auditLogRepository.deleteLogsBefore(beforeDate);
            // log.info("Logs de auditoría antiguos eliminados antes de: {}", beforeDate);
        } catch (Exception e) {
            log.error("Error al limpiar logs de auditoría antiguos: {}", e.getMessage(), e);
        }
    }
}
