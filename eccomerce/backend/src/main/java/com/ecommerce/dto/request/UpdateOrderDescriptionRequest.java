package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderDescriptionRequest {

    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 1000, message = "La descripción no puede tener más de 1000 caracteres")
    private String description;
}