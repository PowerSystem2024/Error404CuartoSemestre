package com.ecommerce.service.impl;

import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.UserRole;
import com.ecommerce.model.entity.EmailNotification;
import com.ecommerce.repository.EmailNotificationRepository;
import com.ecommerce.repository.EmailVerificationTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.interfaces.OrderService;
import com.ecommerce.service.interfaces.ProductReviewService;
import com.ecommerce.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProductReviewService productReviewService;
    private final OrderService orderService;
    private final EmailNotificationRepository emailNotificationRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Obteniendo usuario por ID: {}", id);

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Obteniendo usuario por email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmailOrUsername(String emailOrUsername) {
        log.debug("Obteniendo usuario por email o username: {}", emailOrUsername);

        return userRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con email o username: " + emailOrUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Obteniendo todos los usuarios - Página: {}, Tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User changeUserRole(Long userId, UserRole newRole, String adminEmail) {
        // log.info("Cambio de rol solicitado - UserID: {}, Nuevo rol: {}, Admin: {}",
        // userId, newRole, adminEmail);

        // Verificar que el admin existe y es administrador
        User admin = getUserByEmailOrUsername(adminEmail);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo los administradores pueden cambiar roles de usuarios");
        }

        // Obtener el usuario a modificar
        User user = getUserById(userId);

        // No permitir que un admin se quite su propio rol de admin
        if (user.getId().equals(admin.getId()) && newRole != UserRole.ADMIN) {
            throw new IllegalArgumentException("No puedes quitarte el rol de administrador a ti mismo");
        }

        user.setRole(newRole);
        User savedUser = userRepository.save(user);

        // log.info("Rol de usuario cambiado exitosamente - UserID: {}, Rol anterior:
        // {}, Nuevo rol: {}",
        // userId, oldRole, newRole);

        return savedUser;
    }

    @Override
    @Transactional
    public User toggleUserStatus(Long userId, boolean active, String adminEmail) {
        // log.info("Cambio de estado de usuario - UserID: {}, Activo: {}, Admin: {}",
        // userId, active, adminEmail);

        // Verificar que el admin existe y es administrador
        User admin = getUserByEmailOrUsername(adminEmail);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo los administradores pueden cambiar el estado de usuarios");
        }

        // Obtener el usuario a modificar
        User user = getUserById(userId);

        // No permitir que un admin se desactive a sí mismo
        if (user.getId().equals(admin.getId()) && !active) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }

        // Cambiar el estado active y actualizar auditoría
        user.setActive(active);

        if (!active) {
            // Soft delete: actualizar campos de auditoría
            user.setDeletedAt(LocalDateTime.now());
            user.setDeletedBy(admin.getUsername() != null ? admin.getUsername() : admin.getEmail());
        } else {
            // Reactivación: limpiar campos de auditoría
            user.setDeletedAt(null);
            user.setDeletedBy(null);
        }

        User savedUser = userRepository.save(user);

        // log.info("Estado de usuario cambiado exitosamente - UserID: {}, Nuevo estado:
        // {}, Admin: {}",
        // userId, active, adminEmail);

        return savedUser;
    }

    @Override
    @Transactional
    public User changePassword(Long userId, String newPassword, String adminEmail) {
        // log.info("Cambio de contraseña solicitado - UserID: {}, Admin: {}", userId,
        // adminEmail);

        // Verificar que el admin existe y es administrador
        User admin = getUserByEmailOrUsername(adminEmail);
        if (admin.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Solo los administradores pueden cambiar contraseñas de usuarios");
        }

        // Obtener el usuario a modificar
        User user = getUserById(userId);

        // Codificar la nueva contraseña
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        User savedUser = userRepository.save(user);

        // log.info("Contraseña de usuario cambiada exitosamente - UserID: {}, Admin:
        // {}", userId, adminEmail);

        return savedUser;
    }

    @Override
    @Transactional
    public void hardDeleteUser(Long userId, String adminEmail) {
        log.warn("=== INICIANDO HARD DELETE - UserID: {}, Admin: {} ===", userId, adminEmail);

        try {
            // Verificar que el admin existe y es administrador
            log.debug("Paso 1: Verificando admin...");
            User admin = getUserByEmailOrUsername(adminEmail);
            log.debug("Admin encontrado: ID={}, Role={}, Active={}", admin.getId(), admin.getRole(), admin.getActive());
            if (admin.getRole() != UserRole.ADMIN) {
                log.error("Admin no tiene permisos suficientes: Role={}", admin.getRole());
                throw new AccessDeniedException("Solo los administradores pueden eliminar usuarios permanentemente");
            }
            log.debug("Paso 1 completado: Admin verificado correctamente");

            log.debug("Paso 2: Buscando usuario a eliminar...");
            User user = getUserById(userId);
            log.debug("Usuario encontrado: ID={}, Active={}, Email={}", user.getId(), user.getActive(),
                    user.getEmail());
            log.debug("Paso 2 completado: Usuario encontrado");

            // No permitir que un admin se elimine a sí mismo
            if (user.getId().equals(admin.getId())) {
                log.error("Intento de auto-eliminación detectado: UserID={}, AdminID={}", userId, admin.getId());
                throw new IllegalArgumentException("No puedes eliminarte a ti mismo");
            }
            log.debug("Paso 3 completado: Verificación de auto-eliminación pasada");

            // Eliminar todas las reseñas del usuario con auditoría
            log.debug("Paso 4: Eliminando reseñas del usuario...");
            try {
                productReviewService.hardDeleteUserReviews(user.getId(),
                        admin.getUsername() != null ? admin.getUsername() : admin.getEmail());
                log.debug("Paso 4 completado: Reseñas eliminadas");
            } catch (Exception e) {
                log.error("Error al eliminar reseñas del usuario {}: {}", user.getId(), e.getMessage(), e);
                // Continuar con la eliminación del usuario aunque falle la eliminación de
                // reseñas
            }

            // Eliminar todas las órdenes del usuario con auditoría
            log.debug("Paso 5: Eliminando órdenes del usuario...");
            try {
                orderService.hardDeleteUserOrders(user.getId(),
                        admin.getUsername() != null ? admin.getUsername() : admin.getEmail());
                log.debug("Paso 5 completado: Órdenes eliminadas");
            } catch (Exception e) {
                log.error("Error al eliminar órdenes del usuario {}: {}", user.getId(), e.getMessage(), e);
                // Continuar con la eliminación del usuario aunque falle la eliminación de
                // órdenes
            }

            // Eliminar todas las notificaciones de email del usuario
            log.debug("Paso 5.5: Eliminando notificaciones de email del usuario...");
            try {
                List<EmailNotification> notifications = emailNotificationRepository
                        .findByUserIdAndSentFalse(user.getId());
                if (!notifications.isEmpty()) {
                    emailNotificationRepository.deleteAll(notifications);
                    log.debug("Eliminadas {} notificaciones de email del usuario {}", notifications.size(),
                            user.getId());
                } else {
                    log.debug("No se encontraron notificaciones de email para el usuario {}", user.getId());
                }
                log.debug("Paso 5.5 completado: Notificaciones de email eliminadas");
            } catch (Exception e) {
                log.error("Error al eliminar notificaciones de email del usuario {}: {}", user.getId(), e.getMessage(),
                        e);
                // Continuar con la eliminación del usuario aunque falle la eliminación de
                // notificaciones
            }

            // Eliminar tokens de verificación de email del usuario
            log.debug("Paso 5.6: Eliminando tokens de verificación de email del usuario...");
            try {
                emailVerificationTokenRepository.deleteAllByUserId(user.getId());
                log.debug("Eliminados todos los tokens de verificación de email del usuario {}", user.getId());
                log.debug("Paso 5.6 completado: Tokens de verificación de email eliminados");
            } catch (Exception e) {
                log.error("Error al eliminar tokens de verificación de email del usuario {}: {}", user.getId(),
                        e.getMessage(), e);
                // Continuar con la eliminación del usuario aunque falle la eliminación de
                // tokens
            }

            // Actualizar campos de auditoría antes de eliminar
            log.debug("Paso 6: Actualizando campos de auditoría...");
            user.setDeletedAt(LocalDateTime.now());
            user.setDeletedBy(admin.getUsername() != null ? admin.getUsername() : admin.getEmail());
            userRepository.save(user);
            log.debug("Paso 6 completado: Campos de auditoría actualizados");

            // Eliminar permanentemente
            log.debug("Paso 7: Eliminando usuario permanentemente...");
            userRepository.delete(user);
            log.debug("Paso 7 completado: Usuario eliminado");

            log.warn("=== HARD DELETE COMPLETADO EXITOSAMENTE - UserID: {}, Admin: {} ===", userId, adminEmail);

        } catch (Exception e) {
            log.error("=== ERROR EN HARD DELETE - UserID: {}, Admin: {}, Error: {} ===", userId, adminEmail,
                    e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        log.debug("Buscando usuarios - Término: '{}', Página: {}, Tamaño: {}",
                searchTerm, pageable.getPageNumber(), pageable.getPageSize());

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllUsers(pageable);
        }

        return userRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        searchTerm, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStats getUserStats() {
        log.debug("Obteniendo estadísticas de usuarios");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long verifiedUsers = userRepository
                .countByEmailVerificationStatus(com.ecommerce.model.enums.EmailVerificationStatus.VERIFIED);
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);

        return new UserStats(totalUsers, activeUsers, verifiedUsers, adminUsers);
    }
}