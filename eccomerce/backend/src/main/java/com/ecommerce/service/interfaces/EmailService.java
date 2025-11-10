package com.ecommerce.service.interfaces;

import com.ecommerce.model.entity.AuditLog;
import com.ecommerce.model.entity.EmailVerificationToken;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.NotificationType;

public interface EmailService {

    /**
     * Envía un email de verificación al usuario
     */
    void sendVerificationEmail(User user);

    /**
     * Verifica el token de verificación de email
     */
    boolean verifyEmailToken(String token);

    /**
     * Envía una notificación por email
     */
    void sendNotification(User user, NotificationType type, String subject, String content);

    /**
     * Reenvía el email de verificación
     */
    void resendVerificationEmail(String email);

    /**
     * Genera un nuevo token de verificación
     */
    EmailVerificationToken generateVerificationToken(User user);

    /**
     * Procesa el envío de emails pendientes (para ser usado por un scheduler)
     */
    void processPendingEmails();

    /**
     * Envía un email de reset de contraseña
     */
    void sendPasswordResetEmail(User user, String resetToken);

    /**
     * Verifica el token de reset de contraseña
     */
    boolean verifyPasswordResetToken(String token);

    /**
     * Genera un token de reset de contraseña
     */
    String generatePasswordResetToken(User user);

    /**
     * Envía notificación de hard delete a todos los administradores
     */
    void sendHardDeleteNotificationToAdmins(String adminEmail, String entityType, String entityId, String details,
            AuditLog auditLog);

    /**
     * Envía email de confirmación de pago aprobado
     */
    void sendPaymentConfirmationEmail(Order order);
}