package com.iitsm.infrastructure.security;

import com.iitsm.application.port.TokenPort;
import com.iitsm.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Service responsible for generating and validating stateless JSON Web Tokens (JWT).
 * Enforces production security checks: minimum 256-bit key length and blocks committed secret in prod.
 */
@Component
public class JwtTokenProvider implements TokenPort {

    public static final String COMMITTED_DEFAULT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${iitsm.security.jwt.secret:}") String secret,
            @Value("${iitsm.security.jwt.expiration-ms:900000}") long expirationMs,
            Environment environment) {

        Objects.requireNonNull(environment, "environment cannot be null");

        if (secret == null || secret.trim().isEmpty() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes (256 bits). Configure iitsm.security.jwt.secret.");
        }

        if (environment.matchesProfiles("prod") && COMMITTED_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("Production environment cannot use the committed default JWT secret! Set a secure JWT_SECRET environment variable.");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        int version = (user != null) ? user.getTokenVersion() : 1;

        return Jwts.builder()
                .subject(user.getEmail().value())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .claim("tokenVersion", version)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public int getTokenVersionFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Integer v = claims.get("tokenVersion", Integer.class);
        return (v != null) ? v : 1;
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

