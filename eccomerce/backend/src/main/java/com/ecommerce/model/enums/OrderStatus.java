package com.ecommerce.model.enums;

public enum OrderStatus {
    PENDING("Pendiente"),
    CONFIRMED("Confirmado"),
    PROCESSING("En proceso"),
    SHIPPED("Enviado"),
    DELIVERED("Entregado"),
    CANCELLED("Cancelado"),
    REFUNDED("Reembolsado");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}