package com.ecommerce.security.jwt;

import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.userdetails.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public AuthTokenFilter(JwtUtils jwtUtils, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("AuthTokenFilter processing request: {}", requestURI);

        // For public endpoints, skip JWT processing but continue with filter chain
        if (isPublicEndpoint(requestURI)) {
            log.debug("Skipping JWT processing for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Processing JWT authentication for endpoint: {}", requestURI);

        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                Long userId = jwtUtils.getUserIdFromJwtToken(jwt);

                // Verificar que el usuario existe en BD
                if (!userRepository.existsById(userId)) {
                    log.warn("User with ID {} does not exist in database", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                String firstName = jwtUtils.getUserFirstNameFromJwtToken(jwt);
                String lastName = jwtUtils.getUserLastNameFromJwtToken(jwt);
                String role = jwtUtils.getUserRoleFromJwtToken(jwt);

                // Crear UserDetails desde el JWT sin consultar BD
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                UserDetailsImpl userDetails = new UserDetailsImpl(userId, username, null, firstName, lastName,
                        authorities);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    private boolean isPublicEndpoint(String requestURI) {
        boolean isPublic = requestURI.equals("/auth/login") ||
                requestURI.equals("/auth/register") ||
                requestURI.equals("/auth/refresh") ||
                requestURI.startsWith("/public/") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/swagger-resources") ||
                requestURI.startsWith("/webjars") ||
                requestURI.startsWith("/configuration") ||
                requestURI.startsWith("/actuator") ||
                requestURI.equals("/favicon.ico") ||
                requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/images/");

        log.debug("Checking if endpoint {} is public: {}", requestURI, isPublic);
        return isPublic;
    }
}
