package com.ecommerce.exception;

public class AdminPermissionException extends RuntimeException {

    public AdminPermissionException(String message) {
        super(message);
    }

    public AdminPermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}