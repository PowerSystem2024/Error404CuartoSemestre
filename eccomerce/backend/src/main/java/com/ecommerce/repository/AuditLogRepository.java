package com.ecommerce.repository;

import com.ecommerce.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Buscar por usuario
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    Page<AuditLog> findByUserEmailOrderByTimestampDesc(String userEmail, Pageable pageable);

    // Buscar por tipo de entidad
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    // Buscar por acción
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    // Buscar por rango de fechas
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    // Buscar por éxito/fallo
    Page<AuditLog> findBySuccessOrderByTimestampDesc(Boolean success, Pageable pageable);

    // Buscar logs de error
    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.timestamp DESC")
    Page<AuditLog> findErrorLogs(Pageable pageable);

    // Buscar por IP
    Page<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);

    // Estadísticas de auditoría
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action AND a.timestamp >= :since")
    Long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.success = false AND a.timestamp >= :since")
    Long countErrorsSince(@Param("since") LocalDateTime since);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since GROUP BY a.action")
    List<Object[]> getActionStatsSince(@Param("since") LocalDateTime since);

    // Limpiar logs antiguos (útil para mantenimiento)
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :beforeDate")
    void deleteLogsBefore(@Param("beforeDate") LocalDateTime beforeDate);

    // Buscar logs relacionados con una entidad específica
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType,
            String entityId,
            Pageable pageable);

    // Búsqueda avanzada con múltiples filtros
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userEmail IS NULL OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :userEmail, '%'))) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:entityId IS NULL OR a.entityId = :entityId) AND " +
            "(:success IS NULL OR a.success = :success) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> findFilteredAuditLogs(
            @Param("userEmail") String userEmail,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("success") Boolean success,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
