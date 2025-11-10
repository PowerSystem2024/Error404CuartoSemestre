package com.ecommerce.model.enums;

public enum EmailVerificationStatus {
    UNVERIFIED("No verificado"),
    VERIFIED("Verificado"),
    PENDING("Pendiente");

    private final String description;

    EmailVerificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}