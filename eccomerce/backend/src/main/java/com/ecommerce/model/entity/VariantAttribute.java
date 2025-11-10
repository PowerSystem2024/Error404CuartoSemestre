package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "variant_attributes")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttribute extends BaseEntity {

    @Column(nullable = false)
    private String name; // e.g., "Color", "Talla", "Tamaño", "Material", "Estilo"

    @Column(nullable = false)
    private String displayName; // e.g., "Color", "Talla", "Tamaño", "Material", "Estilo"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AttributeType type; // TEXT, NUMBER, SELECT, BOOLEAN

    @Column
    private String unit; // e.g., "cm", "kg", "ml" for NUMBER type

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column
    @Builder.Default
    private Integer sortOrder = 0;

    // Global attributes available for all categories
    @Column(nullable = false)
    @Builder.Default
    private Boolean global = false;

    public enum AttributeType {
        TEXT, // Free text input
        NUMBER, // Numeric input with optional unit
        SELECT, // Dropdown with predefined options
        BOOLEAN // Yes/No checkbox
    }
}