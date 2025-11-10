package com.ecommerce.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_variant_attributes", uniqueConstraints = @UniqueConstraint(columnNames = { "category_id",
        "attribute_id" }))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryVariantAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private VariantAttribute attribute;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column
    @Builder.Default
    private Integer sortOrder = 0;

    // Override display name for this category if needed
    @Column
    private String customDisplayName;

    // Override options for this category if needed
    @ElementCollection
    @CollectionTable(name = "category_attribute_options", joinColumns = @JoinColumn(name = "category_attribute_id"))
    @Column(name = "option_value")
    private java.util.List<String> customOptions;

    public String getEffectiveDisplayName() {
        return customDisplayName != null ? customDisplayName : attribute.getDisplayName();
    }

    public java.util.List<String> getEffectiveOptions() {
        return customOptions != null && !customOptions.isEmpty() ? customOptions : java.util.Collections.emptyList();
    }
}