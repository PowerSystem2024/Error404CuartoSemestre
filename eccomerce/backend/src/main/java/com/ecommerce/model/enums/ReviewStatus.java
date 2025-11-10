package com.ecommerce.model.enums;

public enum ReviewStatus {
    PENDING("Pendiente"),
    APPROVED("Aprobado"),
    REJECTED("Rechazado");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
