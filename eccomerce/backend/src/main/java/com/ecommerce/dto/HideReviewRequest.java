package com.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitar ocultamiento de una reseña
 */
@Schema(description = "Request para ocultar una reseña con razón")
public class HideReviewRequest {

    @NotBlank(message = "La razón no puede estar vacía")
    @Size(min = 5, max = 500, message = "La razón debe tener entre 5 y 500 caracteres")
    @Schema(description = "Razón por la cual se oculta la reseña", example = "Contiene lenguaje ofensivo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    public HideReviewRequest() {
    }

    public HideReviewRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "HideReviewRequest{" +
                "reason='" + reason + '\'' +
                '}';
    }
}
