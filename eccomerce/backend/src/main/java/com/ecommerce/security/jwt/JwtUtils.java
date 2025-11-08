package com.ecommerce.security.jwt;

import com.ecommerce.repository.ProfileRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.userdetails.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtils {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("id", userPrincipal.getId())
                .claim("firstName", userPrincipal.getFirstName())
                .claim("lastName", userPrincipal.getLastName())
                .claim("email", userPrincipal.getEmail())
                .claim("role", userPrincipal.getAuthorities().iterator().next().getAuthority())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenFromUsername(String username) {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return Jwts.builder()
                .setSubject(username)
                .claim("id", user.getId())
                .claim("firstName", profile.getFirstName())
                .claim("lastName", profile.getLastName())
                .claim("role", "ROLE_" + user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public Long getUserIdFromJwtToken(String token) {
        JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims.get("id", Long.class);
    }

    public String getUserFirstNameFromJwtToken(String token) {
        JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims.get("firstName", String.class);
    }

    public String getUserLastNameFromJwtToken(String token) {
        JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims.get("lastName", String.class);
    }

    public String getUserRoleFromJwtToken(String token) {
        JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();
        Claims claims = parser.parseClaimsJws(token).getBody();
        return claims.get("role", String.class);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            JwtParser parser = Jwts.parser().setSigningKey(getSigningKey()).build();
            parser.parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }
}
