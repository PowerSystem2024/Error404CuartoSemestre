package com.ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HardDeleteRequest {

    @NotNull(message = "Entity ID is required")
    private Long id;

    @NotBlank(message = "Password confirmation is required")
    private String confirmationPassword;
}