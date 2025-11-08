package com.ecommerce.controller.auth;

import com.ecommerce.dto.request.ChangePasswordRequest;
import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.dto.response.ProfileResponse;
import com.ecommerce.security.jwt.JwtUtils;
import com.ecommerce.service.interfaces.AuthService;
import com.ecommerce.service.interfaces.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Perfil", description = "Endpoints de perfil de usuario")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;
    private final JwtUtils jwtUtils;

    private Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtils.getUserIdFromJwtToken(token);
        }
        throw new RuntimeException("Token no encontrado");
    }

    @PutMapping
    @Operation(summary = "Actualizar perfil", description = "Actualiza el perfil del usuario autenticado")
    public ResponseEntity<MessageResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "profile-update");
        MDC.put("sessionId", "profile-update");

        log.info("Profile update attempt");

        try {
            long startTime = System.currentTimeMillis();
            Long userId = getUserIdFromToken(request);
            MessageResponse response = profileService.updateProfile(userId, updateProfileRequest);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Profile update successful for user: {} in {}ms", userId, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Profile update failed - Error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    @Operation(summary = "Obtener perfil", description = "Obtiene el perfil del usuario autenticado")
    public ResponseEntity<ProfileResponse> getProfile(HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "profile-get");
        MDC.put("sessionId", "profile-get");

        log.info("Profile get attempt");

        try {
            long startTime = System.currentTimeMillis();
            Long userId = getUserIdFromToken(request);
            ProfileResponse response = profileService.getProfile(userId);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Profile get successful for user: {} in {}ms", userId, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Profile get failed - Error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping
    @Operation(summary = "Desactivar cuenta propia", description = "Desactiva la cuenta del usuario autenticado")
    public ResponseEntity<MessageResponse> deactivateAccount(HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "account-deactivate");
        MDC.put("sessionId", "account-deactivate");

        log.info("Account deactivation attempt");

        try {
            long startTime = System.currentTimeMillis();
            Long userId = getUserIdFromToken(request);
            MessageResponse response = profileService.deactivateAccount(userId);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Account deactivation successful for user: {} in {}ms", userId, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Account deactivation failed - Error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/password")
    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "password-change");
        MDC.put("sessionId", "password-change");

        log.info("Password change attempt");

        try {
            long startTime = System.currentTimeMillis();
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            MessageResponse response = authService.changePassword(email, request.getCurrentPassword(),
                    request.getNewPassword());
            long duration = System.currentTimeMillis() - startTime;

            log.info("Password change successful for user: {} in {}ms", email, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Password change failed - Error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }
}