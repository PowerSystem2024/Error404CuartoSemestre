package com.ecommerce.model.enums;

public enum NotificationType {
    ORDER_CONFIRMED("Pedido confirmado"),
    ORDER_SHIPPED("Pedido enviado"),
    ORDER_DELIVERED("Pedido entregado"),
    PAYMENT_RECEIVED("Pago recibido"),
    ACCOUNT_VERIFIED("Cuenta verificada"),
    PASSWORD_RESET("Restablecimiento de contraseña"),
    WELCOME("Bienvenido"),
    ADMIN_ALERT("Alerta administrativa");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}