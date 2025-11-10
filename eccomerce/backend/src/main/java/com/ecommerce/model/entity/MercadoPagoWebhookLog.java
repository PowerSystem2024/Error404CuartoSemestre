package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mp_webhook_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoWebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String webhookId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String action;

    @Column(length = 2000)
    private String data;

    @Column(length = 1000)
    private String headers;

    @Column
    private String signature;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column
    private String processingError;

    @Column
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
