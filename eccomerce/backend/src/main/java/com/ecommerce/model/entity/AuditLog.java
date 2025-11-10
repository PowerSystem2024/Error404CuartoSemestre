package com.ecommerce.model.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String userEmail;
    private String action;
    private String entityType;
    private String entityId;
    @Column(length = 1000)
    private String details;
    private Boolean success;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private String ipAddress;
    @Column(length = 500)
    private String userAgent;
    private Long executionTimeMs;
    private LocalDateTime timestamp;
    private String requestMethod;
    @Column(length = 1000)
    private String requestUrl;
}
