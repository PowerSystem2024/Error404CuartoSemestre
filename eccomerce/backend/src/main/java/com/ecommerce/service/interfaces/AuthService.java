package com.ecommerce.service.interfaces;

import com.ecommerce.dto.request.LoginRequest;
import com.ecommerce.dto.request.RegisterRequest;
import com.ecommerce.dto.response.JwtResponse;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.model.entity.User;

public interface AuthService {

    JwtResponse authenticateUser(LoginRequest loginRequest);

    MessageResponse registerUser(RegisterRequest registerRequest);

    JwtResponse refreshToken(String token);

    /**
     * Verifica si un usuario puede iniciar sesión (email verificado y activo)
     */
    boolean canUserLogin(String email);

    /**
     * Verifica si un usuario puede iniciar sesión (email verificado y activo)
     */
    boolean canUserLogin(User user);

    /**
     * Cambia la contraseña del usuario autenticado
     */
    MessageResponse changePassword(String email, String currentPassword, String newPassword);

    /**
     * Inicia el proceso de recuperación de contraseña enviando un email
     */
    MessageResponse forgotPassword(String email);

    /**
     * Resetea la contraseña usando el token enviado por email
     */
    MessageResponse resetPassword(String token, String newPassword);
}
