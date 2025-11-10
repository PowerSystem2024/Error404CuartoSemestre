package com.ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private Long orderId;
    private BigDecimal amount;
    private String paymentMethodId;
    private String description;

    // Campos para URLs de retorno personalizadas
    private String autoReturn;
    private java.util.Map<String, String> backUrls;

    // Campos adicionales para preferencias simples
    private String origin;
}
