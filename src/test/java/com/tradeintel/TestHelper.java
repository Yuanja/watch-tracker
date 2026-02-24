package com.tradeintel;

import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Shared test helper utilities for creating test users, generating JWT tokens,
 * and computing HMAC-SHA256 webhook signatures.
 *
 * <p>This class is deliberately a plain utility class (not a Spring component)
 * so that individual test classes can use it without coupling themselves to a
 * Spring context lifecycle.</p>
 */
public final class TestHelper {

    private static final Logger log = LogManager.getLogger(TestHelper.class);

    private TestHelper() {}

    // -------------------------------------------------------------------------
    // User creation
    // -------------------------------------------------------------------------

    /**
     * Persists a new {@link User} with the given email and role, then returns
     * the saved entity (with its generated UUID populated by JPA).
     *
     * @param userRepository the repository to save into
     * @param email          unique email for the test user
     * @param role           the role to assign
     * @return the saved {@link User} entity
     */
    public static User createUser(UserRepository userRepository, String email, UserRole role) {
        User user = new User();
        user.setGoogleId("google-" + email.hashCode());
        user.setEmail(email);
        user.setDisplayName("Test User " + email);
        user.setAvatarUrl("https://example.com/avatar.png");
        user.setRole(role);
        user.setIsActive(true);
        User saved = userRepository.save(user);
        log.debug("Created test user id={} email={} role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return saved;
    }

    /**
     * Generates a signed JWT Bearer token value for the given user.
     * The returned string does NOT include the "Bearer " prefix.
     *
     * @param tokenProvider the JWT provider configured with the test secret
     * @param user          the user entity to encode into the token
     * @return compact JWT string (no "Bearer " prefix)
     */
    public static String generateToken(JwtTokenProvider tokenProvider, User user) {
        return tokenProvider.generateToken(user);
    }

    /**
     * Returns the full {@code Authorization} header value including the "Bearer " prefix.
     *
     * @param tokenProvider the JWT provider
     * @param user          the user entity
     * @return e.g. "Bearer eyJ..."
     */
    public static String bearerHeader(JwtTokenProvider tokenProvider, User user) {
        return "Bearer " + generateToken(tokenProvider, user);
    }

    // -------------------------------------------------------------------------
    // HMAC-SHA256 webhook signature computation
    // -------------------------------------------------------------------------

    /**
     * Computes the HMAC-SHA256 hex digest of {@code body} using {@code secret},
     * matching the algorithm used by {@link com.tradeintel.webhook.WhapiSignatureValidator}.
     *
     * @param body   the raw request body string
     * @param secret the shared webhook secret
     * @return lowercase hex HMAC-SHA256 digest
     * @throws IllegalStateException if the JVM does not support HmacSHA256
     */
    public static String computeHmacSignature(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }
}
