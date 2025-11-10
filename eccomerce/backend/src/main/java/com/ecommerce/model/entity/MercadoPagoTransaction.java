package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mp_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // IDs principales
    @Column(unique = true)
    private String paymentId;

    @Column
    private String collectionId;

    @Column
    private String merchantOrderId;

    @Column
    private String preferenceId;

    // Relación con Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Estados y información básica
    @Column(nullable = false)
    private String status;

    @Column
    private String collectionStatus;

    @Column
    private String processingMode;

    @Column
    private String siteId;

    // Información de monto
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal transactionAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal couponAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal installmentAmount;

    @Column
    @Builder.Default
    private String currency = "ARS";

    // Información de método de pago
    @Column(length = 1000)
    private String paymentMethod;

    @Column
    private String paymentTypeId;

    @Column(length = 4)
    private String lastFourDigits;

    @Column
    private String issuer;

    @Column
    private Integer installments;

    @Column
    private String cardId;

    @Column(length = 500)
    private String cardTokenId;

    // Información de la transacción
    @Column(length = 2000)
    private String description;

    @Column(length = 500)
    private String statement;

    @Column
    private String authorizationCode;

    @Column
    private String nsuProcessingMode;

    @Column
    private String receiptNumber;

    // Información de pagar y receptor
    @Column
    private String payerEmail;

    @Column
    private String payerName;

    @Column
    private String payerIdType;

    @Column
    private String payerId;

    @Column
    private String receiverEmail;

    // Referencias externas
    @Column
    private String externalReference;

    // JSON para datos dinámicos adicionales
    @Column(columnDefinition = "TEXT")
    private String paymentData;

    // Fechas importantes
    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column
    private LocalDateTime refundedAt;

    @Column
    private LocalDateTime dateCreated;

    @Column
    private LocalDateTime dateApproved;

    @Column
    private LocalDateTime dateLastUpdated;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
