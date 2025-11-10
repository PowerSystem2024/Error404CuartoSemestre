package com.ecommerce.service.impl;

import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.model.entity.EmailNotification;
import com.ecommerce.model.entity.EmailVerificationToken;
import com.ecommerce.model.entity.PasswordResetToken;
import com.ecommerce.model.entity.Profile;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.enums.EmailVerificationStatus;
import com.ecommerce.model.enums.NotificationType;
import com.ecommerce.model.enums.UserRole;
import com.ecommerce.repository.EmailNotificationRepository;
import com.ecommerce.repository.EmailVerificationTokenRepository;
import com.ecommerce.repository.PasswordResetTokenRepository;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.service.interfaces.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailNotificationRepository notificationRepository;
    private final TemplateEngine templateEngine;
    private final AuditService auditService;

    @Value("${app.email.verification.expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.email.from:noreply@ecommerce.com}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.api.verify-email-path}")
    private String verifyEmailPath;

    @Value("${app.frontend.login-path}")
    private String loginPath;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Override
    @Transactional
    public void sendVerificationEmail(User user) {
        log.info("📧 Enviando email de verificación a: {} (ID: {})", user.getEmail(), user.getId());

        // Generar token en transacción separada
        EmailVerificationToken token;
        try {
            token = generateVerificationToken(user);
        } catch (Exception e) {
            log.error("Error generando token para usuario {}: {}", user.getEmail(), e.getMessage());
            throw e;
        }

        // Crear contenido del email usando template
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getEmail()));
        String subject = "Verifica tu cuenta - E-commerce";
        String htmlContent = buildVerificationEmailHtml(profile.getFirstName(), token.getToken());

        log.info("📨 URL de verificación generada: {}/verify-email?token={}...",
                frontendUrl, token.getToken().substring(0, Math.min(token.getToken().length(), 10)));

        // Crear notificación
        EmailNotification notification = EmailNotification.builder()
                .user(user)
                .type(NotificationType.ACCOUNT_VERIFIED)
                .subject(subject)
                .content(htmlContent)
                .recipientEmail(user.getEmail())
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        // Enviar email HTML (fuera de transacción para no rollbackear el token)
        try {
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            log.info("✅ Email de verificación enviado exitosamente a: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Error enviando email a {}: {}", user.getEmail(), e.getMessage());
            // Registrar en auditoría el fallo del envío
            auditService.logEvent(
                    "EMAIL_SEND_FAILED",
                    "USER",
                    user.getId().toString(),
                    "Fallo envío email verificación a " + user.getEmail(),
                    false,
                    e.getMessage());
            // No throw, para no rollbackear el token
        }
    }

    @Override
    @Transactional
    public boolean verifyEmailToken(String token) {
        log.info("Verificando token de email - Longitud: {}, Primeros 10 caracteres: {}",
                token.length(), token.substring(0, Math.min(token.length(), 10)));

        // Buscar el token
        Optional<EmailVerificationToken> verificationTokenOpt = tokenRepository.findByToken(token);

        if (verificationTokenOpt.isEmpty()) {
            log.error("Token NO encontrado en la base de datos: {}", token);
            // Verificar si existe algún token similar
            long totalTokens = tokenRepository.count();
            log.error("Total de tokens en BD: {}", totalTokens);
            throw new ResourceNotFoundException("Token de verificación no encontrado o inválido");
        }

        EmailVerificationToken verificationToken = verificationTokenOpt.get();
        User user = verificationToken.getUser();

        log.info("Token encontrado para usuario: {} (ID: {}), Estado: used={}, expires={}",
                user.getEmail(), user.getId(), verificationToken.getUsed(), verificationToken.getExpiryDate());

        // Si el token ya fue usado
        if (verificationToken.getUsed()) {
            // Si la cuenta ya está activa, está todo bien
            if (user.getActive() && user.getEmailVerificationStatus() == EmailVerificationStatus.VERIFIED) {
                log.info("Email ya verificado para usuario: {}", user.getEmail());
                return true;
            }
            // Si el token fue usado pero la cuenta NO está activa, permitir re-verificar
            log.info("Re-verificando email para usuario inactivo: {}", user.getEmail());
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Token expirado para usuario {}: expira={}, ahora={}",
                    user.getEmail(), verificationToken.getExpiryDate(), LocalDateTime.now());
            return false;
        }

        // Marcar token como usado
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);
        log.info("Token marcado como usado para usuario: {}", user.getEmail());

        // Actualizar estado del usuario
        user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        user.setActive(true); // Activar la cuenta
        userRepository.save(user);

        log.info("✅ Email verificado EXITOSAMENTE para usuario: {} - Estado actualizado a VERIFIED y active=true",
                user.getEmail());
        return true;
    }

    @Override
    @Transactional
    public void sendNotification(User user, NotificationType type, String subject, String content) {
        // log.info("Enviando notificación {} a: {}", type, user.getEmail());

        // Crear contenido HTML usando template
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getEmail()));
        String htmlContent = buildNotificationEmailHtml(profile.getFirstName(), subject, content);

        EmailNotification notification = EmailNotification.builder()
                .user(user)
                .type(type)
                .subject(subject)
                .content(htmlContent)
                .recipientEmail(user.getEmail())
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        try {
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            // log.info("Notificación enviada exitosamente");
        } catch (Exception e) {
            log.error("Error enviando notificación a {}: {}", user.getEmail(), e.getMessage());
            // Registrar en auditoría el fallo del envío
            auditService.logEvent(
                    "EMAIL_SEND_FAILED",
                    "USER",
                    user.getId().toString(),
                    "Fallo envío notificación " + type + " a " + user.getEmail(),
                    false,
                    e.getMessage());
            throw e; // Relanzar la excepción ya que este método no maneja tokens
        }
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        // log.info("Reenviando email de verificación a: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getEmailVerificationStatus() == EmailVerificationStatus.VERIFIED) {
            // Enviar email informando que ya está verificado
            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getEmail()));
            String subject = "Tu cuenta ya está verificada - E-commerce";
            String htmlContent = buildAlreadyVerifiedEmailHtml(profile.getFirstName());
            try {
                sendHtmlEmail(user.getEmail(), subject, htmlContent);
                // log.info("Email de 'ya verificado' enviado a: {}", email);
            } catch (Exception e) {
                log.error("Error enviando email de 'ya verificado' a {}: {}", email, e.getMessage());
                // Registrar en auditoría el fallo del envío
                auditService.logEvent(
                        "EMAIL_SEND_FAILED",
                        "USER",
                        user.getId().toString(),
                        "Fallo envío email 'ya verificado' a " + user.getEmail(),
                        false,
                        e.getMessage());
            }
            return;
        }

        // Generar token en transacción separada
        EmailVerificationToken token;
        try {
            token = generateVerificationToken(user);
        } catch (Exception e) {
            log.error("Error generando token para reenvío a {}: {}", email, e.getMessage());
            throw e;
        }

        String subject = "Reenvío de verificación - E-commerce";
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getEmail()));
        String htmlContent = buildResendVerificationEmailHtml(profile.getFirstName(), token.getToken());

        EmailNotification notification = EmailNotification.builder()
                .user(user)
                .type(NotificationType.ACCOUNT_VERIFIED)
                .subject(subject)
                .content(htmlContent)
                .recipientEmail(user.getEmail())
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        // Enviar email HTML
        try {
            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            // log.info("Email de reenvío enviado exitosamente a: {}", email);
        } catch (Exception e) {
            log.error("Error enviando email de reenvío a {}: {}", email, e.getMessage());
            // Registrar en auditoría el fallo del envío
            auditService.logEvent(
                    "EMAIL_SEND_FAILED",
                    "USER",
                    user.getId().toString(),
                    "Fallo envío email reenvío verificación a " + user.getEmail(),
                    false,
                    e.getMessage());
            // No throw
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public EmailVerificationToken generateVerificationToken(User user) {
        log.info("🔑 Generando token de verificación para usuario: {} (ID: {})", user.getEmail(), user.getId());

        // Invalidar tokens anteriores
        tokenRepository.invalidateUserTokens(user.getId());
        log.debug("Tokens anteriores invalidados para usuario: {}", user.getId());

        // Generar nuevo token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        EmailVerificationToken savedToken = tokenRepository.save(verificationToken);
        tokenRepository.flush(); // Forzar persistencia inmediata
        log.info("✅ Token de verificación guardado en BD - Token ID: {}, Token: {}..., Usuario: {}, Expira: {}",
                savedToken.getId(),
                savedToken.getToken().substring(0, Math.min(savedToken.getToken().length(), 10)),
                user.getEmail(),
                savedToken.getExpiryDate());

        // Actualizar el usuario con el token
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        managedUser.setEmailVerificationToken(savedToken.getToken());
        managedUser.setEmailVerificationTokenExpiry(savedToken.getExpiryDate());
        userRepository.save(managedUser);
        log.info("✅ Token actualizado en el registro del usuario: {}", user.getEmail());

        return savedToken;
    }

    @Override
    @Transactional
    public void processPendingEmails() {
        // log.info("Procesando emails pendientes");

        List<EmailNotification> pendingEmails = notificationRepository
                .findBySentFalseAndScheduledAtBefore(LocalDateTime.now());

        for (EmailNotification notification : pendingEmails) {
            try {
                sendHtmlEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getContent());

                notification.setSent(true);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.debug("Email enviado exitosamente - ID: {}", notification.getId());

            } catch (Exception e) {
                log.error("Error enviando email - ID: {}: {}", notification.getId(), e.getMessage());

                notification.setErrorMessage(e.getMessage());
                notificationRepository.save(notification);

                // Registrar en auditoría el fallo del envío
                auditService.logEvent(
                        "EMAIL_SEND_FAILED",
                        "EMAIL_NOTIFICATION",
                        notification.getId().toString(),
                        "Fallo envío email programado a " + notification.getRecipientEmail(),
                        false,
                        e.getMessage());
            }
        }

        // log.info("Procesamiento de emails completado - {} emails procesados",
        // pendingEmails.size());
    }

    private String buildVerificationEmailHtml(String firstName, String token) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("verificationUrl", frontendUrl + "/verify-email?token=" + token);
        context.setVariable("subject", "Verifica tu cuenta");

        String htmlContent = templateEngine.process("email/email-verification", context);
        return wrapWithBaseTemplate(htmlContent, "Verifica tu cuenta");
    }

    private String buildResendVerificationEmailHtml(String firstName, String token) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("verificationUrl", frontendUrl + "/verify-email?token=" + token);
        context.setVariable("subject", "Reenvío de verificación");

        String htmlContent = templateEngine.process("email/email-verification-resend", context);
        return wrapWithBaseTemplate(htmlContent, "Reenvío de verificación");
    }

    private String buildAlreadyVerifiedEmailHtml(String firstName) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("loginUrl", frontendUrl + loginPath);
        context.setVariable("subject", "Cuenta ya verificada");

        String htmlContent = templateEngine.process("email/email-already-verified", context);
        return wrapWithBaseTemplate(htmlContent, "Cuenta ya verificada");
    }

    private String wrapWithBaseTemplate(String content, String subject) {
        Context context = new Context();
        context.setVariable("content", content);
        context.setVariable("subject", subject);

        return templateEngine.process("email/base-template", context);
    }

    private String buildNotificationEmailHtml(String recipientName, String subject, String content) {
        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("subject", subject);
        context.setVariable("content", content);

        String htmlContent = templateEngine.process("email/email-notification", context);
        return wrapWithBaseTemplate(htmlContent, subject);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Envío de email deshabilitado por configuración: {}", to);
            return;
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indica que es HTML

            mailSender.send(message);
            log.debug("Email HTML enviado a: {}", to);

        } catch (Exception e) {
            log.error("Error enviando email HTML a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    @Override
    @Transactional
    public void sendPasswordResetEmail(User user, String resetToken) {
        // log.info("Enviando email de reset de contraseña a: {}", user.getEmail());

        try {
            // Crear contenido del email usando template
            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + user.getEmail()));
            String subject = "Reset de Contraseña - E-commerce";
            String htmlContent = buildPasswordResetEmailHtml(profile.getFirstName(), resetToken);

            // Crear notificación
            EmailNotification notification = EmailNotification.builder()
                    .user(user)
                    .type(NotificationType.PASSWORD_RESET)
                    .subject(subject)
                    .content(htmlContent)
                    .recipientEmail(user.getEmail())
                    .scheduledAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            // Enviar email HTML
            try {
                sendHtmlEmail(user.getEmail(), subject, htmlContent);
                // log.info("Email de reset de contraseña enviado exitosamente a: {}",
                // user.getEmail());
            } catch (Exception e) {
                log.error("Error enviando email de reset a {}: {}", user.getEmail(), e.getMessage());
                // Registrar en auditoría el fallo del envío
                auditService.logEvent(
                        "EMAIL_SEND_FAILED",
                        "USER",
                        user.getId().toString(),
                        "Fallo envío email reset contraseña a " + user.getEmail(),
                        false,
                        e.getMessage());
                // IMPORTANTE: No relanzar la excepción para evitar rollback del token
                log.warn("Token de reset generado pero email no pudo enviarse para: {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Error procesando envío de email de reset para {}: {}", user.getEmail(), e.getMessage());
            // IMPORTANTE: No relanzar la excepción para evitar rollback del token
            log.warn("Token de reset generado pero hubo error procesando email para: {}", user.getEmail());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyPasswordResetToken(String token) {
        log.debug("Verificando token de reset de contraseña: {}", token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null) {
            log.debug("Token de reset no encontrado: {}", token);
            return false;
        }

        if (resetToken.getUsed()) {
            log.debug("Token de reset ya usado: {}", token);
            return false;
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.debug("Token de reset expirado: {}", token);
            return false;
        }

        log.debug("Token de reset válido: {}", token);
        return true;
    }

    @Override
    @Transactional
    public String generatePasswordResetToken(User user) {
        // log.info("Generando token de reset de contraseña para usuario: {}",
        // user.getEmail());

        // Invalidar tokens anteriores del usuario
        passwordResetTokenRepository.deleteByUser(user);

        // Generar nuevo token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        PasswordResetToken savedToken = passwordResetTokenRepository.save(resetToken);
        // passwordResetTokenRepository.flush(); // Forzar persistencia inmediata

        // log.info("Token de reset de contraseña generado para usuario: {}",
        // user.getEmail());
        return savedToken.getToken();
    }

    @Override
    public void sendHardDeleteNotificationToAdmins(String adminEmail, String entityType, String entityId,
            String details, AuditLog auditLog) {
        // log.info("Enviando notificación de hard delete a administradores - Admin: {},
        // Entity: {}, ID: {}", adminEmail,
        // entityType, entityId);

        try {
            // Obtener todos los usuarios con rol ADMIN
            List<User> adminUsers = userRepository.findByRole(UserRole.ADMIN);

            if (adminUsers.isEmpty()) {
                log.warn("No se encontraron usuarios administradores para enviar notificación de hard delete");
                return;
            }

            // Crear contenido del email usando template
            String subject = "ALERTA: Eliminación Permanente - " + entityType + " ID: " + entityId;
            String htmlContent = buildHardDeleteNotificationEmailHtml(adminEmail, entityType, entityId, details,
                    auditLog);

            // Enviar email a cada administrador
            for (User admin : adminUsers) {
                try {
                    // Crear notificación en la base de datos
                    EmailNotification notification = EmailNotification.builder()
                            .user(admin)
                            .type(NotificationType.ADMIN_ALERT)
                            .subject(subject)
                            .content(htmlContent)
                            .recipientEmail(admin.getEmail())
                            .scheduledAt(LocalDateTime.now())
                            .build();

                    notificationRepository.save(notification);

                    // Enviar email inmediatamente
                    sendHtmlEmail(admin.getEmail(), subject, htmlContent);
                    // log.info("Notificación de hard delete enviada a admin: {}",
                    // admin.getEmail());

                } catch (Exception e) {
                    log.error("Error enviando notificación de hard delete a admin {}: {}", admin.getEmail(),
                            e.getMessage());
                    // Registrar en auditoría el fallo del envío
                    auditService.logEvent(
                            "EMAIL_SEND_FAILED",
                            "USER",
                            admin.getId().toString(),
                            "Fallo envío notificación hard delete a admin " + admin.getEmail(),
                            false,
                            e.getMessage());
                    // Continuar con el siguiente admin
                }
            }

        } catch (Exception e) {
            log.error("Error general enviando notificaciones de hard delete: {}", e.getMessage(), e);
            // Registrar en auditoría el error general
            auditService.logEvent(
                    "EMAIL_SEND_FAILED",
                    "SYSTEM",
                    "HARD_DELETE_NOTIFICATION",
                    "Error general enviando notificaciones de hard delete",
                    false,
                    e.getMessage());
        }
    }

    @Override
    public void sendPaymentConfirmationEmail(Order order) {
        log.info("Enviando email de confirmación de pago para orden: {}", order.getOrderNumber());

        try {
            // Obtener el perfil del usuario
            Profile profile = profileRepository.findByUserId(order.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Profile not found for order: " + order.getOrderNumber()));

            String subject = "Confirmación de Pago - Orden " + order.getOrderNumber();
            String htmlContent = buildPaymentConfirmationEmailHtml(order, profile);

            // Crear notificación
            EmailNotification notification = EmailNotification.builder()
                    .user(order.getUser())
                    .type(NotificationType.ORDER_CONFIRMED)
                    .subject(subject)
                    .content(htmlContent)
                    .recipientEmail(order.getUser().getEmail())
                    .scheduledAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);

            // Enviar email HTML
            sendHtmlEmail(order.getUser().getEmail(), subject, htmlContent);
            log.info("Email de confirmación de pago enviado exitosamente para orden: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error enviando email de confirmación de pago para orden {}: {}", order.getOrderNumber(),
                    e.getMessage());
            // Registrar en auditoría el fallo del envío
            auditService.logEvent(
                    "EMAIL_SEND_FAILED",
                    "ORDER",
                    order.getId().toString(),
                    "Fallo envío email confirmación pago orden " + order.getOrderNumber(),
                    false,
                    e.getMessage());
            // No throw para no interrumpir el flujo de pago
        }
    }

    private String buildPaymentConfirmationEmailHtml(Order order, Profile profile) {
        Context context = new Context();
        context.setVariable("firstName", profile.getFirstName());
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("orderDate", order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate().toString()
                : LocalDateTime.now().toLocalDate().toString());
        context.setVariable("paymentMethod",
                order.getPaymentMethod() != null ? order.getPaymentMethod() : "MercadoPago");
        context.setVariable("orderItems", order.getItems());
        context.setVariable("subtotal", order.getSubtotal());
        context.setVariable("taxAmount", order.getTaxAmount());
        context.setVariable("shippingAmount", order.getShippingAmount());
        context.setVariable("discountAmount", order.getDiscountAmount());
        context.setVariable("totalAmount", order.getTotalAmount());
        context.setVariable("shippingAddress", order.getShippingAddress());
        context.setVariable("ordersUrl", frontendUrl + "/orders");
        context.setVariable("baseUrl", baseUrl);

        return templateEngine.process("payment-confirmation", context);
    }

    private String buildPasswordResetEmailHtml(String firstName, String resetToken) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("resetToken", resetToken);
        context.setVariable("resetUrl", baseUrl + "/reset-password?" + resetToken);
        context.setVariable("baseUrl", baseUrl);

        return templateEngine.process("password-reset-email", context);
    }

    private String buildHardDeleteNotificationEmailHtml(String adminEmail, String entityType, String entityId,
            String details, AuditLog auditLog) {
        Context context = new Context();
        context.setVariable("adminEmail", adminEmail);
        context.setVariable("entityType", entityType);
        context.setVariable("entityId", entityId);
        context.setVariable("details", details);
        context.setVariable("auditLog", auditLog);
        context.setVariable("timestamp", LocalDateTime.now());
        context.setVariable("baseUrl", baseUrl);

        return templateEngine.process("hard-delete-notification", context);
    }

}
