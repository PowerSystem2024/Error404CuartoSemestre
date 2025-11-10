package com.ecommerce.config;

import com.ecommerce.service.interfaces.AuditService;
import com.ecommerce.service.interfaces.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private final AuditService auditService;
    private final UserService userService;

    // Rutas que no queremos loguear (estáticas, health checks, etc.)
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/configuration/ui",
            "/configuration/security",
            "/favicon.ico",
            "/css/",
            "/js/",
            "/images/",
            "/webjars/");

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? requestURI + "?" + queryString : requestURI;

        // Obtener información del cliente
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        // Log inicial de la petición
        log.info("Petición HTTP: {} {} desde {} - User-Agent: {}",
                method, fullUrl, clientIp,
                userAgent != null ? userAgent.substring(0, Math.min(100, userAgent.length())) : "unknown");

        try {
            // Continuar con la cadena de filtros
            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatus();
            // Determinar si fue exitoso
            boolean success = statusCode >= 200 && statusCode < 400;

            // Log del resultado
            if (success) {
                log.info("Respuesta HTTP: {} {} - Estado: {} - Duración: {}ms",
                        method, requestURI, statusCode, duration);
            } else {
                log.warn("Respuesta HTTP con error: {} {} - Estado: {} - Duración: {}ms",
                        method, requestURI, statusCode, duration);
            }

            // Registrar en auditoría (solo para rutas importantes)
            if (!shouldExcludePath(requestURI)) {
                try {
                    // Obtener información del usuario autenticado
                    Long userId = null;
                    String userEmail = null;

                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && authentication.isAuthenticated() &&
                            !"anonymousUser".equals(authentication.getPrincipal())) {
                        try {
                            String email = authentication.getName();
                            var user = userService.getUserByEmailOrUsername(email);
                            userId = user.getId();
                            userEmail = user.getEmail();
                        } catch (Exception e) {
                            log.warn("No se pudo obtener información del usuario para auditoría: {}", e.getMessage());
                        }
                    }

                    auditService.logHttpRequest(method, fullUrl, clientIp, userAgent,
                            duration, success,
                            success ? null : "HTTP " + statusCode, userId, userEmail);

                } catch (Exception e) {
                    log.error("Error al registrar petición HTTP: {}", e.getMessage());
                }
            }
        }
    }

    private boolean shouldExcludePath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(excluded -> path.startsWith(excluded));
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

    @Override
    protected boolean shouldNotFilter(@org.springframework.lang.NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // No filtrar requests OPTIONS (CORS preflight)
        return "OPTIONS".equals(request.getMethod()) ||
        // No filtrar archivos estáticos comunes
                path.endsWith(".css") || path.endsWith(".js") ||
                path.endsWith(".png") || path.endsWith(".jpg") ||
                path.endsWith(".jpeg") || path.endsWith(".gif") ||
                path.endsWith(".ico") || path.endsWith(".svg");
    }
}
