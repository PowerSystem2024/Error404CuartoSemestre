package com.ecommerce.exception;

import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.service.interfaces.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

        private final AuditService auditService;

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationExceptions(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                log.warn("Error de validación ocurrido: {}", ex.getMessage());

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getFieldErrors().forEach((error) -> {
                        String fieldName = error.getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                        log.warn("Error de validación para el campo '{}': {}", fieldName, errorMessage);
                });

                // Registrar en auditoría (truncar mensaje largo)
                String truncatedErrorMessage = ex.getMessage() != null && ex.getMessage().length() > 500
                                ? ex.getMessage().substring(0, 497) + "..."
                                : ex.getMessage();
                auditEvent("VALIDATION_ERROR", "VALIDATION", null,
                                "Validation failed for " + errors.size() + " fields",
                                request, false, truncatedErrorMessage);

                return ResponseEntity.badRequest().body(errors);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<MessageResponse> handleBadCredentialsException(
                        BadCredentialsException ex,
                        HttpServletRequest request) {

                log.warn("Intento de credenciales incorrectas desde IP: {}", getClientIpAddress(request));

                // Registrar intento de login fallido
                auditEvent("LOGIN_FAILED", "AUTH", null,
                                "Failed login attempt",
                                request, false, "Invalid credentials");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new MessageResponse("Credenciales inválidas"));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<MessageResponse> handleIllegalArgumentException(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {

                log.warn("Argumento inválido: {}", ex.getMessage());

                auditEvent("ILLEGAL_ARGUMENT", "VALIDATION", null,
                                ex.getMessage(),
                                request, false, ex.getMessage());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new MessageResponse(ex.getMessage()));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<MessageResponse> handleResourceNotFoundException(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                log.warn("Recurso no encontrado: {}", ex.getMessage());

                auditEvent("RESOURCE_NOT_FOUND", "VALIDATION", null,
                                ex.getMessage(),
                                request, false, ex.getMessage());

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new MessageResponse(ex.getMessage()));
        }

        @ExceptionHandler(SecurityException.class)
        public ResponseEntity<MessageResponse> handleSecurityException(
                        SecurityException ex,
                        HttpServletRequest request) {

                log.warn("Excepción de seguridad desde IP {}: {}", getClientIpAddress(request), ex.getMessage());

                auditEvent("SECURITY_VIOLATION", "SECURITY", null,
                                "Security violation detected",
                                request, false, ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new MessageResponse("Acceso denegado"));
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<MessageResponse> handleRuntimeException(
                        RuntimeException ex,
                        HttpServletRequest request) {

                log.error("Excepción de runtime ocurrida: {}", ex.getMessage(), ex);

                auditEvent("RUNTIME_ERROR", "SYSTEM", null,
                                "Runtime exception: " + ex.getClass().getSimpleName(),
                                request, false, ex.getMessage());

                // Para errores de autenticación, devolver el mensaje específico
                String errorMessage = ex.getMessage();
                if (errorMessage != null && (errorMessage.contains("Usuario no encontrado") ||
                                errorMessage.contains("Cuenta no verificada") ||
                                errorMessage.contains("inactiva"))) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new MessageResponse(errorMessage));
                }

                // Para otros errores de runtime, usar mensaje genérico
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new MessageResponse("Error interno del servidor"));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<MessageResponse> handleException(
                        Exception ex,
                        HttpServletRequest request) {

                log.error("Excepción inesperada ocurrida: {}", ex.getMessage(), ex);

                auditEvent("UNEXPECTED_ERROR", "SYSTEM", null,
                                "Unexpected exception: " + ex.getClass().getSimpleName(),
                                request, false, ex.getMessage());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new MessageResponse("Error interno del servidor"));
        }

        private void auditEvent(String action, String entityType, String entityId,
                        String details, HttpServletRequest request,
                        boolean success, String errorMessage) {
                try {
                        // Obtener información del usuario si está autenticado
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        String userEmail = null;
                        Long userId = null;

                        if (authentication != null && authentication.isAuthenticated() &&
                                        !"anonymousUser".equals(authentication.getPrincipal())) {
                                userEmail = authentication.getName();
                                // Aquí podrías obtener el userId del contexto si está disponible
                        }

                        // Construir detalles adicionales
                        String fullDetails = details + " | IP: " + getClientIpAddress(request) +
                                        " | User-Agent: " + request.getHeader("User-Agent") +
                                        " | URL: " + request.getRequestURI() +
                                        " | Method: " + request.getMethod();

                        if (userEmail != null) {
                                auditService.logUserAction(userId, userEmail, action, entityType,
                                                entityId, fullDetails, success, errorMessage);
                        } else {
                                auditService.logEvent(action, entityType, entityId, fullDetails, success, errorMessage);
                        }

                } catch (Exception auditException) {
                        log.error("Error al auditar evento de excepción: {}", auditException.getMessage());
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
