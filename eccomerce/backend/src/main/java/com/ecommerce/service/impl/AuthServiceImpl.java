package com.ecommerce.service.impl;

import com.ecommerce.dto.request.LoginRequest;
import com.ecommerce.dto.request.RegisterRequest;
import com.ecommerce.dto.response.JwtResponse;
import com.ecommerce.dto.response.MessageResponse;
import com.ecommerce.event.EmailVerificationEvent;
import com.ecommerce.model.entity.PasswordResetToken;
import com.ecommerce.model.entity.Profile;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.LoginMethod;
import com.ecommerce.model.enums.EmailVerificationStatus;
import com.ecommerce.model.enums.UserRole;
import com.ecommerce.repository.PasswordResetTokenRepository;
import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.jwt.JwtUtils;
import com.ecommerce.service.interfaces.AuthService;
import com.ecommerce.service.interfaces.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // Buscar usuario por email o username
        User user = userRepository.findByEmailOrUsername(loginRequest.getIdentifier(), loginRequest.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el usuario pueda iniciar sesión
        if (!canUserLogin(user)) {
            throw new RuntimeException("Cuenta no verificada o inactiva. Revisa tu email para verificar tu cuenta.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new JwtResponse(jwt);
    }

    @Override
    @Transactional
    public MessageResponse registerUser(RegisterRequest registerRequest) {
        // log.info("Solicitud de registro para email: {}", registerRequest.getEmail());

        // Validar email
        if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }

        if (!registerRequest.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }

        // Validar username
        if (registerRequest.getUsername() != null && !registerRequest.getUsername().trim().isEmpty()) {
            if (registerRequest.getUsername().length() < 3) {
                throw new IllegalArgumentException("El nombre de usuario debe tener al menos 3 caracteres");
            }
            if (registerRequest.getUsername().length() > 50) {
                throw new IllegalArgumentException("El nombre de usuario no puede tener más de 50 caracteres");
            }
            if (!registerRequest.getUsername().matches("^[a-zA-Z0-9_]+$")) {
                throw new IllegalArgumentException(
                        "El nombre de usuario solo puede contener letras, números y guiones bajos");
            }
        }

        // Validar contraseña
        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        if (registerRequest.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");
        }

        if (registerRequest.getPassword().length() > 100) {
            throw new IllegalArgumentException("La contraseña no puede tener más de 100 caracteres");
        }

        // Verificar si el email ya existe
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Intento de registro con email ya existente: {}", registerRequest.getEmail());
            throw new IllegalArgumentException(
                    "Ya existe una cuenta con este email. Si olvidaste tu contraseña, utiliza la opción 'Olvidé mi contraseña'");
        }

        // Verificar si el username ya existe (si se proporcionó)
        if (registerRequest.getUsername() != null
                && userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            log.warn("Intento de registro con username ya existente: {}", registerRequest.getUsername());
            throw new IllegalArgumentException("Este nombre de usuario ya está en uso. Por favor elige otro");
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .username(registerRequest.getUsername())
                .password(encoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                // .emailVerificationStatus(EmailVerificationStatus.UNVERIFIED)
                .loginMethod(LoginMethod.PASSWORD)
                .active(true)
                .build();

        userRepository.save(user);

        // Crear perfil vacío inicialmente
        Profile profile = Profile.builder()
                .user(user)
                .firstName("")
                .lastName("")
                .phone("")
                .build();

        profileRepository.save(profile);

        // Publicar evento para enviar email de verificación después del commit
        // eventPublisher.publishEvent(new EmailVerificationEvent(user.getId(), user.getEmail()));

        return new MessageResponse("Usuario registrado exitosamente! Revisa tu email para verificar tu cuenta.");
    }

    @Override
    public JwtResponse refreshToken(String token) {
        String jwt = token.substring(7); // Remove "Bearer " prefix
        String username = jwtUtils.getUserNameFromJwtToken(jwt);

        String newToken = jwtUtils.generateTokenFromUsername(username);

        return new JwtResponse(newToken);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserLogin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return canUserLogin(user);
    }

    @Transactional(readOnly = true)
    public boolean canUserLogin(User user) {
        return user.getActive();
            // && user.getEmailVerificationStatus() == EmailVerificationStatus.VERIFIED;
    }

    @Override
    @Transactional
    public MessageResponse changePassword(String email, String currentPassword, String newPassword) {
        // log.info("Cambio de contraseña solicitado para usuario: {}", email);

        // Buscar usuario
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        // log.info("Contraseña cambiada exitosamente para usuario: {}", email);
        return new MessageResponse("Contraseña cambiada exitosamente");
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(String email) {
        // log.info("Solicitud de reset de contraseña para: {}", email);

        // Buscar usuario
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el usuario esté activo y verificado
        if (!canUserLogin(email)) {
            throw new RuntimeException("Cuenta no verificada o inactiva");
        }

        // Generar y guardar token de reset
        String resetToken = emailService.generatePasswordResetToken(user);
        // log.info("Token de reset generado exitosamente para usuario: {}", email);

        // Intentar enviar email
        try {
            emailService.sendPasswordResetEmail(user, resetToken);
            // log.info("Email de reset de contraseña enviado exitosamente a: {}", email);
            return new MessageResponse("Se ha enviado un email con las instrucciones para resetear tu contraseña");
        } catch (Exception e) {
            log.warn("Email de reset no pudo enviarse a {}, pero token generado exitosamente", email);
            // Retornar respuesta indicando que el token está listo aunque el email falló
            return new MessageResponse(
                    "Token de reset generado exitosamente. Contacta al administrador si no recibes el email.");
        }
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(String token, String newPassword) {
        // log.info("Reset de contraseña solicitado con token");

        // Verificar token
        if (!emailService.verifyPasswordResetToken(token)) {
            throw new RuntimeException("Token inválido o expirado");
        }

        // Buscar token en la base de datos
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        // Actualizar contraseña
        User user = resetToken.getUser();
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        // Marcar token como usado
        passwordResetTokenRepository.markTokenAsUsed(token, LocalDateTime.now());

        // log.info("Contraseña reseteada exitosamente para usuario: {}",
        // user.getEmail());
        return new MessageResponse("Contraseña reseteada exitosamente");
    }

}
