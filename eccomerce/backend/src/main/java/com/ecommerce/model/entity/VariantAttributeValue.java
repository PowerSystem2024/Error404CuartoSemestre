package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "variant_attribute_values")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttributeValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private VariantAttribute attribute;

    // The actual value - can be text, number, or selected option
    @Column(nullable = false)
    private String value;

    // For numeric values, store the parsed number
    @Column
    private Double numericValue;

    // For boolean values
    @Column
    private Boolean booleanValue;

    @PrePersist
    @PreUpdate
    private void parseValues() {
        if (value != null) {
            try {
                if (attribute.getType() == VariantAttribute.AttributeType.NUMBER) {
                    numericValue = Double.parseDouble(value);
                } else if (attribute.getType() == VariantAttribute.AttributeType.BOOLEAN) {
                    booleanValue = Boolean.parseBoolean(value);
                }
            } catch (NumberFormatException e) {
                // Invalid number, keep as string
                numericValue = null;
            }
        }
    }

    public String getDisplayValue() {
        switch (attribute.getType()) {
            case NUMBER:
                if (numericValue != null && attribute.getUnit() != null) {
                    return numericValue + " " + attribute.getUnit();
                }
                return value;
            case BOOLEAN:
                return booleanValue != null && booleanValue ? "Sí" : "No";
            default:
                return value;
        }
    }
}