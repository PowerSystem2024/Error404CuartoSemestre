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
public class CategoryVariantConfigRequest {

    private Long categoryId;
    private List<VariantAttributeAssignment> attributes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAttributeAssignment {
        private Long attributeId;
        private Boolean required;
        private Boolean active;
        private Integer sortOrder;
        private String customDisplayName;
        private List<String> customOptions;
    }
}