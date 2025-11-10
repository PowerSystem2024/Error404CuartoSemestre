package com.ecommerce.model.enums;

public enum PaymentStatus {
    PENDING("Pendiente"),
    APPROVED("Aprobado"),
    REJECTED("Rechazado"),
    CANCELLED("Cancelado"),
    REFUNDED("Reembolsado");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}