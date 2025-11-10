package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private Long productId;
    private String sku;
    private String name;
    private String displayName;
    private String description;
    private BigDecimal priceAdjustment;
    private BigDecimal finalPrice;
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}