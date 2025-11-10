package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeResponse {

    private Long id;
    private String name;
    private String displayName;
    private String type;
    private String unit;
    // private List<String> options;
    private Boolean required;
    private Boolean global;
    private Boolean active;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}