package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRequest {

    @NotBlank(message = "El identificador (email o username) es obligatorio")
    private String identifier;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
