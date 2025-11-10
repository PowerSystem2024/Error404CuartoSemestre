package com.ecommerce.model.enums;

public enum ProductStatus {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    OUT_OF_STOCK("Sin stock"),
    DISCONTINUED("Descontinuado");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}