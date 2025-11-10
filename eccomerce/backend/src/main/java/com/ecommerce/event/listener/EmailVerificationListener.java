package com.ecommerce.event.listener;

import com.ecommerce.event.EmailVerificationEvent;
import com.ecommerce.service.interfaces.EmailService;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class EmailVerificationListener {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationListener.class);
    private final UserRepository userRepository;
    private final EmailService emailService;

    public EmailVerificationListener(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @Async // opcional, requiere @EnableAsync
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailVerificationEvent(EmailVerificationEvent event) {
        log.info("Procesando evento de verificación de email para usuario ID: {} y email: {}", event.userId(),
                event.email());
        try {
            Optional<User> userOpt = userRepository.findById(event.userId());
            if (userOpt.isPresent()) {
                log.info("Enviando email de verificación (después de commit) a: {}", event.email());
                emailService.sendVerificationEmail(userOpt.get());
            } else {
                log.error("No se encontró el usuario con ID {} para enviar email de verificación", event.userId());
            }
        } catch (Exception ex) {
            log.error("Error enviando email de verificación a {}: {}", event.email(), ex.getMessage(), ex);
        }
    }
}
