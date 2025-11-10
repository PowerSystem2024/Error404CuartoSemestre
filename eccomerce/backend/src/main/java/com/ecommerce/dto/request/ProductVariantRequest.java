package com.ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    private String sku;
    private String name;
    private String description;
    private BigDecimal priceAdjustment;
    private Integer stockQuantity;
    private String imageUrl;
    private Boolean active;
    private Integer sortOrder;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;

    // Dynamic attributes as key-value pairs
    private Map<String, String> attributes;

    // Backward compatibility
    private String color;
    private String size;
    private String material;
}