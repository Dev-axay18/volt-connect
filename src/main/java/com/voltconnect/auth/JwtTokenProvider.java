package com.voltconnect.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Handles all JWT operations for the Volt-Connect backend.
 *
 * <p>Uses JJWT 0.12.3 with HMAC-SHA256 signing. The secret is loaded from
 * {@code jwt.secret} (base64-encoded), expiry from {@code jwt.expiry} (ms),
 * and refresh expiry from {@code jwt.refresh-expiry} (ms).
 *
 * <p>Satisfies Requirements 1.2, 1.5, 1.6, 20.1, 20.2.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE  = "type";
    private static final String TYPE_ACCESS  = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long      expiryMs;
    private final long      refreshExpiryMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiry}") long expiryMs,
            @Value("${jwt.refresh-expiry}") long refreshExpiryMs) {

        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signingKey      = Keys.hmacShaKeyFor(keyBytes);
        this.expiryMs        = expiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    // ── Token generation ─────────────────────────────────────────────────────

    /**
     * Generates a signed access JWT for the given user.
     *
     * @param userId the subject (Firebase UID / user UUID)
     * @param roles  list of role strings (e.g. ["driver", "host"])
     * @return compact JWT string
     */
    public String generateAccessToken(String userId, List<String> roles) {
        Date now        = new Date();
        Date expiration = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a signed refresh JWT for the given user.
     *
     * @param userId the subject
     * @return compact JWT string
     */
    public String generateRefreshToken(String userId) {
        Date now        = new Date();
        Date expiration = new Date(now.getTime() + refreshExpiryMs);

        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    // ── Token validation ─────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the token is structurally valid, correctly signed,
     * and not expired. Returns {@code false} for any invalid/expired/tampered token
     * — never throws.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Claims extraction ────────────────────────────────────────────────────

    /**
     * Extracts the subject (userId) from the token.
     * Caller must ensure the token is valid before calling this.
     */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the roles claim from the token as a {@code List<String>}.
     * Caller must ensure the token is valid before calling this.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    /**
     * Returns {@code true} if the token's {@code type} claim equals {@code "access"}.
     */
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * Returns {@code true} if the token's {@code type} claim equals {@code "refresh"}.
     */
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
