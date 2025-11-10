package com.ecommerce.controller.admin;

import com.ecommerce.dto.request.HardDeleteRequest;
import com.ecommerce.dto.request.ChangeRoleRequest;
import com.ecommerce.dto.request.ToggleUserStatusRequest;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.model.entity.Profile;
import com.ecommerce.model.entity.User;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.service.interfaces.AuditService;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Usuarios", description = "Endpoints de administración de usuarios")
public class AdminUserController {
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios", description = "Obtiene la lista paginada de todos los usuarios")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", authentication.getName());

        log.info("Admin obteniendo todos los usuarios - Página: {}, Tamaño: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userService.getAllUsers(pageable);

            Page<UserResponse> userResponses = users.map(this::convertToUserResponse);

            log.info("Usuarios obtenidos exitosamente - Cantidad: {}", users.getNumberOfElements());

            return ResponseEntity.ok(userResponses);

        } catch (Exception e) {
            log.error("Error al obtener usuarios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Page.empty());
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar usuarios", description = "Busca usuarios por nombre o email")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "admin");

        log.info("Admin buscando usuarios - Término: '{}', Página: {}, Tamaño: {}", searchTerm, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userService.searchUsers(searchTerm, pageable);

            Page<UserResponse> userResponses = users.map(this::convertToUserResponse);

            log.info("Búsqueda completada - {} resultados encontrados", users.getNumberOfElements());

            return ResponseEntity.ok(userResponses);

        } catch (Exception e) {
            log.error("Error al buscar usuarios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Page.empty());
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Cambiar rol de usuario", description = "Cambia el rol de un usuario específico")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", authentication.getName());

        log.info("Admin cambiando rol de usuario - UserID: {}, Nuevo rol: {}", userId, request.getRole());

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            User admin = userService.getUserByEmailOrUsername(adminEmail);

            User updatedUser = userService.changeUserRole(userId, request.getRole(), authentication.getName());
            UserResponse response = convertToUserResponse(updatedUser);

            // Registrar auditoría exitosa
            auditService.logUserAction(
                    admin.getId(),
                    admin.getEmail(),
                    "CHANGE_USER_ROLE",
                    "USER",
                    userId.toString(),
                    "Cambio de rol: " + request.getRole() + " para usuario ID: " + userId,
                    true);

            log.info("Rol de usuario cambiado exitosamente - UserID: {}, Nuevo rol: {}", userId, request.getRole());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al cambiar rol de usuario - UserID: {}: {}", userId, e.getMessage(), e);

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "CHANGE_USER_ROLE",
                        "USER",
                        userId.toString(),
                        "Intento fallido de cambio de rol: " + request.getRole() + " para usuario ID: " + userId,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "Cambiar estado de usuario", description = "Activa o desactiva un usuario")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long userId,
            @RequestBody ToggleUserStatusRequest request,
            Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", authentication.getName());

        log.info("Admin cambiando estado de usuario - UserID: {}, Activo: {}", userId, request.isActive());

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            User admin = userService.getUserByEmailOrUsername(adminEmail);

            User updatedUser = userService.toggleUserStatus(userId, request.isActive(), authentication.getName());
            UserResponse response = convertToUserResponse(updatedUser);

            // Registrar auditoría exitosa
            String action = request.isActive() ? "ACTIVATE_USER" : "DEACTIVATE_USER";
            String details = (request.isActive() ? "Activación" : "Desactivación") + " de usuario ID: " + userId;
            auditService.logUserAction(
                    admin.getId(),
                    admin.getEmail(),
                    action,
                    "USER",
                    userId.toString(),
                    details,
                    true);

            log.info("Estado de usuario cambiado exitosamente - UserID: {}, Activo: {}", userId, request.isActive());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al cambiar estado de usuario - UserID: {}: {}", userId, e.getMessage(), e);

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                User admin = userService.getUserByEmailOrUsername(adminEmail);
                String action = request.isActive() ? "ACTIVATE_USER" : "DEACTIVATE_USER";
                String details = "Intento fallido de " + (request.isActive() ? "activación" : "desactivación")
                        + " de usuario ID: " + userId;
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        action,
                        "USER",
                        userId.toString(),
                        details,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene los detalles de un usuario específico")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId, Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", authentication.getName());

        log.info("Admin obteniendo usuario por ID: {}", userId);

        try {
            User user = userService.getUserById(userId);
            UserResponse response = convertToUserResponse(user);

            log.info("Usuario obtenido exitosamente - ID: {}", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener usuario - ID: {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Obtener estadísticas de usuarios", description = "Obtiene estadísticas generales de usuarios")
    public ResponseEntity<UserService.UserStats> getUserStats(Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", authentication.getName());

        log.info("Admin obteniendo estadísticas de usuarios");

        try {
            UserService.UserStats stats = userService.getUserStats();

            log.info("Estadísticas obtenidas - Total: {}, Activos: {}, Verificados: {}, Admins: {}",
                    stats.totalUsers(), stats.activeUsers(), stats.verifiedUsers(), stats.adminUsers());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error al obtener estadísticas de usuarios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(null);
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar usuario", description = "Desactiva un usuario (borrado lógico)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        log.info("Admin desactivando usuario - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            User admin = userService.getUserByEmailOrUsername(adminEmail);

            userService.toggleUserStatus(id, false, authentication.getName());

            // Registrar auditoría exitosa
            auditService.logUserAction(
                    admin.getId(),
                    admin.getEmail(),
                    "SOFT_DELETE_USER",
                    "USER",
                    id.toString(),
                    "Desactivación (soft delete) del usuario ID: " + id,
                    true);

            log.info("Usuario desactivado exitosamente - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al desactivar usuario {}: {}", id, e.getMessage());

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "SOFT_DELETE_USER",
                        "USER",
                        id.toString(),
                        "Intento fallido de desactivación del usuario ID: " + id,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/eliminar")
    @Operation(summary = "Eliminar usuario permanentemente", description = "Elimina permanentemente un usuario de la base de datos")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable Long id, Authentication authentication) {
        log.warn("Admin eliminando usuario permanentemente - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            User admin = userService.getUserByEmailOrUsername(adminEmail);

            // Registrar auditoría antes de la eliminación
            log.debug("Registrando auditoría para hard delete - Admin ID: {}, Email: {}, User ID: {}", admin.getId(),
                    admin.getEmail(), id);
            AuditLog auditLog = auditService.logUserActionAndReturn(
                    admin.getId(),
                    admin.getEmail(),
                    "HARD_DELETE_USER",
                    "USER",
                    id.toString(),
                    "Eliminación permanente del usuario ID: " + id,
                    true);
            log.debug("Auditoría registrada exitosamente para hard delete");

            userService.hardDeleteUser(id, authentication.getName());

            // Enviar notificación a todos los administradores
            try {
                emailService.sendHardDeleteNotificationToAdmins(
                        admin.getEmail(),
                        "USER",
                        id.toString(),
                        "El administrador " + admin.getEmail() + " eliminó permanentemente al usuario ID: " + id,
                        auditLog);
                log.info("Notificación de hard delete enviada a administradores");
            } catch (Exception e) {
                log.error("Error enviando notificación de hard delete: {}", e.getMessage());
                // No fallar la operación por error en notificación
            }
            log.warn("Usuario eliminado permanentemente - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar usuario permanentemente {}: {}", id, e.getMessage());

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_USER",
                        "USER",
                        id.toString(),
                        "Intento fallido de eliminación permanente del usuario ID: " + id,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Reactivar usuario", description = "Reactiva un usuario previamente desactivado")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id, Authentication authentication) {
        log.info("Admin reactivando usuario - ID: {}", id);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            User admin = userService.getUserByEmailOrUsername(adminEmail);

            userService.toggleUserStatus(id, true, authentication.getName());

            // Registrar auditoría exitosa
            auditService.logUserAction(
                    admin.getId(),
                    admin.getEmail(),
                    "RESTORE_USER",
                    "USER",
                    id.toString(),
                    "Reactivación del usuario ID: " + id,
                    true);

            log.info("Usuario reactivado exitosamente - ID: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error reactivando usuario {}: {}", id, e.getMessage());

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "RESTORE_USER",
                        "USER",
                        id.toString(),
                        "Intento fallido de reactivación del usuario ID: " + id,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{userId}/hard-delete")
    @Operation(summary = "Eliminar usuario permanentemente", description = "Elimina permanentemente un usuario de la base de datos (requiere contraseña del admin)")
    public ResponseEntity<Void> hardDeleteUser(
            @PathVariable Long userId,
            @Valid @RequestBody HardDeleteRequest request,
            Authentication authentication) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", authentication.getName());

        log.warn("Admin intentando eliminar usuario permanentemente - UserID: {}", userId);

        try {
            // Obtener información del admin que realiza la acción
            String adminEmail = authentication.getName();
            User admin = userService.getUserByEmailOrUsername(adminEmail);

            // Verificar que la contraseña proporcionada sea correcta
            if (!passwordEncoder.matches(request.getConfirmationPassword(), admin.getPassword())) {
                log.warn("Contraseña incorrecta para hard delete de usuario - Admin: {}, UserID: {}", adminEmail,
                        userId);

                // Registrar auditoría del intento fallido
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_USER",
                        "USER",
                        userId.toString(),
                        "Intento fallido de eliminación permanente - contraseña incorrecta",
                        false);

                return ResponseEntity.status(401).build(); // Unauthorized
            }

            // Verificar que no se esté eliminando a sí mismo
            if (admin.getId().equals(userId)) {
                log.warn("Admin intentando eliminarse a sí mismo - Admin: {}", adminEmail);

                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_USER",
                        "USER",
                        userId.toString(),
                        "Intento fallido de auto-eliminación",
                        false);

                return ResponseEntity.badRequest().build();
            }

            // Registrar auditoría antes de la eliminación
            AuditLog auditLog = auditService.logUserActionAndReturn(
                    admin.getId(),
                    admin.getEmail(),
                    "HARD_DELETE_USER",
                    "USER",
                    userId.toString(),
                    "Eliminación permanente del usuario ID: " + userId,
                    true);
            log.debug("Auditoría registrada exitosamente para hard delete de usuario");

            // Ejecutar la eliminación permanente
            userService.hardDeleteUser(userId, adminEmail);

            // Enviar notificación por email a todos los administradores
            try {
                emailService.sendHardDeleteNotificationToAdmins(
                        admin.getEmail(),
                        "USER",
                        userId.toString(),
                        "El administrador " + admin.getEmail() + " eliminó permanentemente al usuario ID: " + userId,
                        auditLog);
                log.info("Notificación de hard delete de usuario enviada a administradores");
            } catch (Exception e) {
                log.error("Error enviando notificación de hard delete de usuario: {}", e.getMessage());
            }

            log.warn("Usuario eliminado permanentemente - ID: {}", userId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error al eliminar usuario permanentemente - UserID: {}: {}", userId, e.getMessage(), e);

            // Registrar auditoría del error
            try {
                String adminEmail = authentication.getName();
                User admin = userService.getUserByEmailOrUsername(adminEmail);
                auditService.logUserAction(
                        admin.getId(),
                        admin.getEmail(),
                        "HARD_DELETE_USER",
                        "USER",
                        userId.toString(),
                        "Error en eliminación permanente del usuario ID: " + userId,
                        false,
                        e.getMessage());
            } catch (Exception auditException) {
                log.error("Error al registrar auditoría del fallo: {}", auditException.getMessage());
            }

            return ResponseEntity.badRequest().build();
        } finally {
            MDC.clear();
        }
    }

    private UserResponse convertToUserResponse(User user) {
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .phone(profile != null ? profile.getPhone() : "")
                .role(user.getRole())
                .emailVerificationStatus(user.getEmailVerificationStatus())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}