package com.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OrderItemRequest {

    private Long productId;
    private Integer quantity;
    private BigDecimal price;

    // Para capturar y loguear propiedades adicionales
    @Builder.Default
    private final Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditional(String name, Object value) {
        log.debug("🔍 OrderItemRequest - Campo adicional: {} = {} (tipo: {})",
                name, value, value != null ? value.getClass().getSimpleName() : "null");
        this.additionalProperties.put(name, value);
    }
}
