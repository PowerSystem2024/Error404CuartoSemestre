package com.ecommerce.model.enums;

/**
 * Enum que define las acciones de auditoría disponibles en el sistema
 */
public enum AuditAction {
    // Autenticación
    LOGIN("Inicio de sesión"),
    LOGOUT("Cierre de sesión"),
    LOGIN_FAILED("Intento de login fallido"),
    PASSWORD_RESET("Restablecimiento de contraseña"),
    PASSWORD_CHANGE("Cambio de contraseña"),

    // Usuarios
    USER_CREATE("Creación de usuario"),
    USER_UPDATE("Actualización de usuario"),
    USER_DELETE("Eliminación de usuario"),
    USER_ACTIVATE("Activación de usuario"),
    USER_DEACTIVATE("Desactivación de usuario"),
    USER_ROLE_CHANGE("Cambio de rol de usuario"),

    // Productos
    PRODUCT_CREATE("Creación de producto"),
    PRODUCT_UPDATE("Actualización de producto"),
    PRODUCT_DELETE("Eliminación de producto"),
    PRODUCT_STOCK_UPDATE("Actualización de stock"),

    // Categorías
    CATEGORY_CREATE("Creación de categoría"),
    CATEGORY_UPDATE("Actualización de categoría"),
    CATEGORY_DELETE("Eliminación de categoría"),

    // Pedidos
    ORDER_CREATE("Creación de pedido"),
    ORDER_UPDATE("Actualización de pedido"),
    ORDER_CANCEL("Cancelación de pedido"),
    ORDER_COMPLETE("Completación de pedido"),
    ORDER_PAYMENT("Pago de pedido"),

    // Carrito de compras
    CART_ADD_ITEM("Agregar item al carrito"),
    CART_REMOVE_ITEM("Remover item del carrito"),
    CART_UPDATE_QUANTITY("Actualizar cantidad en carrito"),
    CART_CLEAR("Vaciar carrito"),

    // Reviews
    REVIEW_CREATE("Creación de reseña"),
    REVIEW_UPDATE("Actualización de reseña"),
    REVIEW_DELETE("Eliminación de reseña"),

    // HTTP Requests
    HTTP_REQUEST("Petición HTTP"),

    // Sistema
    SYSTEM_BACKUP("Respaldo del sistema"),
    SYSTEM_MAINTENANCE("Mantenimiento del sistema"),
    CONFIG_UPDATE("Actualización de configuración");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getActionCode() {
        return this.name();
    }
}