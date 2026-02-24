package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256-signed JWTs used as stateless session tokens.
 *
 * <p>Token claims:
 * <ul>
 *   <li>{@code sub}   — user UUID</li>
 *   <li>{@code email} — user email</li>
 *   <li>{@code role}  — user role string (e.g. "admin")</li>
 *   <li>{@code iat}   — issued-at timestamp</li>
 *   <li>{@code exp}   — expiry timestamp</li>
 * </ul>
 *
 * <p>Configuration keys:
 * <ul>
 *   <li>{@code app.jwt.secret}        — HS256 signing secret (min 256 bits)</li>
 *   <li>{@code app.jwt.expiration-ms} — token lifetime in milliseconds</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LogManager.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE  = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey  = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a signed JWT for the given user.
     *
     * @param user the authenticated user entity
     * @return compact serialized JWT string
     */
    public String generateToken(User user) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE,  user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the token is well-formed, correctly signed, and not expired.
     *
     * @param token compact JWT string
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is null or empty: {}", e.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Claims extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts the user UUID from the token's {@code sub} claim.
     *
     * @param token a valid compact JWT string
     * @return the user's UUID
     */
    public UUID getUserIdFromToken(String token) {
        String subject = parseClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Extracts the user email from the token's {@code email} claim.
     *
     * @param token a valid compact JWT string
     * @return the user's email address
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get(CLAIM_EMAIL, String.class);
    }

    /**
     * Extracts the role name from the token's {@code role} claim.
     *
     * @param token a valid compact JWT string
     * @return the role string (e.g. "admin")
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
