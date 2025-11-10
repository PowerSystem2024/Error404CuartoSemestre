package com.ecommerce.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    private String shortDescription;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "El precio comparativo no puede ser negativo")
    private BigDecimal compareAtPrice;

    @NotNull(message = "La cantidad en stock es obligatoria")
    @Min(value = 0, message = "La cantidad en stock no puede ser negativa")
    private Integer stockQuantity;

    @Min(value = 0, message = "El límite de stock bajo no puede ser negativo")
    private Integer lowStockThreshold;

    private String sku;

    private String barcode;

    private Long categoryId;

    private Boolean active;
    private Boolean featured;

    @Min(value = 0, message = "El orden de destacados debe ser no negativo")
    private Integer featuredOrder;

    private String imageUrl;

    // Campos SEO
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
}