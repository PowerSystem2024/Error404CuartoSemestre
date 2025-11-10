package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = { "product", "attributeValues" })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String sku; // Unique identifier for this variant

    @Column
    private String name; // Optional display name for the variant

    @Column(length = 1000)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal priceAdjustment; // Price difference from base product price

    @Column
    private Integer stockQuantity;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column
    @Builder.Default
    private Integer sortOrder = 0;

    // Weight for shipping calculations
    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    // Dimensions for shipping
    @Column(precision = 10, scale = 2)
    private BigDecimal length;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    @Column(precision = 10, scale = 2)
    private BigDecimal height;

    // Dynamic attributes through separate table
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<VariantAttributeValue> attributeValues;

    // Backward compatibility - will be deprecated
    @Column
    private String color;

    @Column
    private String size;

    @Column
    private String material;

    // Helper methods
    public BigDecimal getFinalPrice() {
        if (product == null || product.getPrice() == null) {
            return priceAdjustment != null ? priceAdjustment : BigDecimal.ZERO;
        }
        return product.getPrice().add(priceAdjustment != null ? priceAdjustment : BigDecimal.ZERO);
    }

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }

        // Generate name from attributes
        StringBuilder sb = new StringBuilder();
        if (attributeValues != null) {
            for (VariantAttributeValue attrValue : attributeValues) {
                if (sb.length() > 0)
                    sb.append(" - ");
                sb.append(attrValue.getDisplayValue());
            }
        }

        // Fallback to legacy fields
        if (sb.length() == 0) {
            if (color != null)
                sb.append(color);
            if (size != null) {
                if (sb.length() > 0)
                    sb.append(" - ");
                sb.append(size);
            }
            if (material != null) {
                if (sb.length() > 0)
                    sb.append(" - ");
                sb.append(material);
            }
        }

        return sb.length() > 0 ? sb.toString() : "Variante";
    }

    public String getAttributeValue(String attributeName) {
        if (attributeValues != null) {
            for (VariantAttributeValue attrValue : attributeValues) {
                if (attrValue.getAttribute().getName().equals(attributeName)) {
                    return attrValue.getValue();
                }
            }
        }
        return null;
    }
}