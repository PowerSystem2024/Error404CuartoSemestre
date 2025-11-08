package com.ecommerce.controller.auth;

import com.ecommerce.dto.request.ForgotPasswordRequest;
import com.ecommerce.dto.request.ResetPasswordRequest;
import com.ecommerce.dto.request.LoginRequest;
import com.ecommerce.dto.request.RegisterRequest;
import com.ecommerce.dto.response.JwtResponse;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints de autenticación")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un token JWT")
    public ResponseEntity<JwtResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String clientIp = getClientIpAddress(request);

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");
        MDC.put("sessionId", "login-attempt");

        log.info("Login attempt for user: {} from IP: {}", loginRequest.getIdentifier(), clientIp);

        try {
            long startTime = System.currentTimeMillis();
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            long duration = System.currentTimeMillis() - startTime;

            // Actualizar MDC con información del usuario autenticado
            MDC.put("userId", loginRequest.getIdentifier());

            log.info("Login successful for user: {} in {}ms", loginRequest.getIdentifier(), duration);

            return ResponseEntity.ok(jwtResponse);

        } catch (Exception e) {
            log.warn("Login failed for user: {} - Error: {}", loginRequest.getIdentifier(), e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario en el sistema")
    public ResponseEntity<MessageResponse> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String clientIp = getClientIpAddress(request);

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");
        MDC.put("sessionId", "registration");

        log.info("User registration attempt for email: {} from IP: {}", registerRequest.getEmail(), clientIp);

        try {
            long startTime = System.currentTimeMillis();
            MessageResponse response = authService.registerUser(registerRequest);
            long duration = System.currentTimeMillis() - startTime;

            log.info("User registration successful for email: {} in {}ms", registerRequest.getEmail(), duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("User registration failed for email: {} - Error: {}", registerRequest.getEmail(), e.getMessage(),
                    e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Genera un nuevo token JWT usando el token actual")
    public ResponseEntity<JwtResponse> refreshToken(
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        Long requestId = System.currentTimeMillis();
        String clientIp = getClientIpAddress(request);

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "token-refresh");
        MDC.put("sessionId", "token-refresh");

        log.info("Token refresh attempt from IP: {}", clientIp);

        try {
            long startTime = System.currentTimeMillis();
            JwtResponse jwtResponse = authService.refreshToken(token);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Token refresh successful in {}ms", duration);

            return ResponseEntity.ok(jwtResponse);

        } catch (Exception e) {
            log.warn("Token refresh failed - Error: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Olvidé mi contraseña", description = "Envía un email con un token para resetear la contraseña")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        Long requestId = System.currentTimeMillis();
        String clientIp = getClientIpAddress(httpRequest);

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");
        MDC.put("sessionId", "forgot-password");

        log.info("Forgot password request for email: {} from IP: {}", request.getEmail(), clientIp);

        try {
            long startTime = System.currentTimeMillis();
            MessageResponse response = authService.forgotPassword(request.getEmail());
            long duration = System.currentTimeMillis() - startTime;

            log.info("Forgot password email sent successfully for: {} in {}ms", request.getEmail(), duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Forgot password failed for {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Resetear contraseña", description = "Resetea la contraseña usando el token enviado por email")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        Long requestId = System.currentTimeMillis();
        String clientIp = getClientIpAddress(httpRequest);

        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");
        MDC.put("sessionId", "reset-password");

        log.info("Reset password request with token from IP: {}", clientIp);

        try {
            long startTime = System.currentTimeMillis();
            MessageResponse response = authService.resetPassword(request.getToken(), request.getNewPassword());
            long duration = System.currentTimeMillis() - startTime;

            log.info("Password reset successful in {}ms", duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/verification-status")
    @Operation(summary = "Estado de verificación", description = "Verifica si el email del usuario está verificado")
    public ResponseEntity<MessageResponse> checkVerificationStatus(@RequestParam String email) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");

        log.info("Checking verification status for email: {}", email);

        try {
            boolean canLogin = authService.canUserLogin(email);

            if (canLogin) {
                log.info("Email verified and account active for: {}", email);
                return ResponseEntity.ok(new MessageResponse("Cuenta verificada y activa"));
            } else {
                log.info("Email not verified or account inactive for: {}", email);
                return ResponseEntity.ok(new MessageResponse("Cuenta no verificada o inactiva"));
            }

        } catch (Exception e) {
            log.error("Error checking verification status for email: {} - Error: {}", email, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error al verificar estado: " + e.getMessage()));
        } finally {
            MDC.clear();
        }
    }

    // Temporary endpoint to create admin user
    @PostMapping("/create-admin")
    @Operation(summary = "Crear usuario admin", description = "Endpoint temporal para crear usuario admin")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> createAdminUser() {
        try {
            RegisterRequest registerRequest = RegisterRequest.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password("admin123")
                    .build();

            MessageResponse response = authService.registerUser(registerRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error creating admin user: " + e.getMessage()));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
