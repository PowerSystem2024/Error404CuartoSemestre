package com.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class OrderRequest {

    private List<OrderItemRequest> items;
    private Long shippingAddressId;
    private Long billingAddressId;
    private String paymentMethod;

    // Campos adicionales para recibir formato del frontend
    @JsonProperty("shippingAddress")
    private String shippingAddressString;

    @JsonProperty("billingAddress")
    private String billingAddressString;

    // Para capturar propiedades adicionales y loguearlas
    @Builder.Default
    private final Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditional(String name, Object value) {
        log.debug("🔍 OrderRequest - Campo adicional recibido: {} = {}", name, value);
        this.additionalProperties.put(name, value);
    }
}