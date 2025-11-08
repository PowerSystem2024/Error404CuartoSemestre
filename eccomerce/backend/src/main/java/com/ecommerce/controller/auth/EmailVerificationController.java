package com.ecommerce.controller.auth;

import com.ecommerce.dto.request.ResendVerificationRequest;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.service.interfaces.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verificación de Email", description = "Endpoints para verificación de email")
public class EmailVerificationController {

    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.frontend.login-path}")
    private String loginPath;

    @Value("${app.frontend.verification-success-path:/verification-success}")
    private String verificationSuccessPath;

    @Value("${app.frontend.verification-error-path:/verification-error}")
    private String verificationErrorPath;

    @GetMapping("/verify-email")
    @Operation(summary = "Verificar email", description = "Verifica el email del usuario usando un token")
    public Object verifyEmail(@RequestParam String token,
            @RequestHeader(value = "Accept", defaultValue = "text/html") String acceptHeader) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");

        log.info("Verificando token de email: {}", token.substring(0, Math.min(token.length(), 10)) + "...");

        try {
            boolean verified = emailService.verifyEmailToken(token);

            if (verified) {
                log.info("Email verificado exitosamente con token");

                // Si es una petición que acepta JSON (como AJAX), devolver respuesta JSON
                if (acceptHeader.contains("application/json")) {
                    return ResponseEntity.ok(new MessageResponse("Email verificado exitosamente. Redirigiendo..."));
                }

                // Si no, redirigir al frontend
                return new RedirectView(frontendUrl + verificationSuccessPath + "?verified=true");
            } else {
                log.warn("Token de verificación inválido o expirado");

                if (acceptHeader.contains("application/json")) {
                    return ResponseEntity.badRequest()
                            .body(new MessageResponse("Token de verificación inválido o expirado"));
                }

                return new RedirectView(frontendUrl + verificationErrorPath + "?error=invalid_token");
            }

        } catch (Exception e) {
            log.error("Error al verificar email: {}", e.getMessage(), e);

            if (acceptHeader.contains("application/json")) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Token inválido o expirado"));
            }

            return new RedirectView(frontendUrl + verificationErrorPath + "?error=invalid_token");
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verificar email (API)", description = "Verifica el email del usuario usando un token y devuelve respuesta JSON")
    public ResponseEntity<MessageResponse> verifyEmailApi(@RequestParam String token) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");

        log.info("Verificando token de email via API: {}", token.substring(0, Math.min(token.length(), 10)) + "...");

        try {
            boolean verified = emailService.verifyEmailToken(token);

            if (verified) {
                log.info("Email verificado exitosamente con token via API");
                return ResponseEntity.ok(new MessageResponse("Email verificado exitosamente"));
            } else {
                log.warn("Token de verificación inválido o expirado via API");
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Token de verificación inválido o expirado"));
            }

        } catch (Exception e) {
            log.error("Error al verificar email via API: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Token inválido o expirado"));
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenviar verificación", description = "Reenvía el email de verificación")
    public ResponseEntity<MessageResponse> resendVerification(@RequestBody ResendVerificationRequest request) {

        Long requestId = System.currentTimeMillis();
        MDC.put("requestId", requestId.toString());
        MDC.put("userId", "anonymous");

        log.info("Reenviando verificación de email a: {}", request.getEmail());

        try {
            emailService.resendVerificationEmail(request.getEmail());

            log.info("Email de verificación reenviado exitosamente");
            return ResponseEntity.ok(new MessageResponse("Email de verificación reenviado exitosamente"));

        } catch (Exception e) {
            log.error("Error al reenviar verificación de email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email no encontrado o ya verificado"));
        } finally {
            MDC.clear();
        }
    }
}