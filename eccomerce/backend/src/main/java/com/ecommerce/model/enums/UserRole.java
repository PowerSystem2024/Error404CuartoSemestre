package com.ecommerce.model.enums;

public enum UserRole {
    USER("Usuario"),
    ADMIN("Administrador");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}