package com.ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeRequest {

    private String name;
    private String displayName;
    private String type; // TEXT, NUMBER, SELECT, BOOLEAN
    private String unit;
    // private List<String> options;
    private Boolean required;
    private Boolean global;
    private Integer sortOrder;
    private List<Long> categoryIds; // Para atributos específicos por categoría
}