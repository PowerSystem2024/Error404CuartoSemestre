package com.ecommerce.service.interfaces;

import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    /**
     * Obtiene un usuario por ID
     */
    User getUserById(Long id);

    /**
     * Obtiene un usuario por email
     */
    User getUserByEmail(String email);

    /**
     * Obtiene un usuario por email o username
     */
    User getUserByEmailOrUsername(String emailOrUsername);

    /**
     * Lista todos los usuarios con paginación
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Cambia el rol de un usuario (solo administradores)
     */
    User changeUserRole(Long userId, UserRole newRole, String adminEmail);

    /**
     * Activa o desactiva un usuario
     */
    User toggleUserStatus(Long userId, boolean active, String adminEmail);

    /**
     * Cambia la contraseña de un usuario (solo administradores)
     */
    User changePassword(Long userId, String newPassword, String adminEmail);

    /**
     * Elimina permanentemente un usuario de la base de datos
     */
    void hardDeleteUser(Long userId, String adminEmail);

    /**
     * Busca usuarios por nombre o email
     */
    Page<User> searchUsers(String searchTerm, Pageable pageable);

    /**
     * Obtiene estadísticas de usuarios
     */
    UserStats getUserStats();

    /**
         * Clase interna para estadísticas de usuarios
         */
        record UserStats(long totalUsers, long activeUsers, long verifiedUsers, long adminUsers) {
    }
}