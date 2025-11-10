package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String preferenceId;
    private String paymentId;
    private String initPoint;
    private String status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // URL para sandbox (desarrollo)
    // private String sandboxInitPoint;

    // URLs de retorno configuradas
    private java.util.Map<String, String> backUrls;

    // Campos adicionales para información detallada del pago
    private String paymentMethodId;
    private String paymentTypeId;
    private OffsetDateTime dateCreated;
    private OffsetDateTime dateApproved;
    private String errorMessage;
    private Integer errorCode;
}
