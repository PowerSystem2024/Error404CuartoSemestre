package com.ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoftDeleteRequest {

    @NotNull(message = "Entity ID is required")
    private Long id;

    @NotBlank(message = "Deletion reason is required")
    private String reason;
}