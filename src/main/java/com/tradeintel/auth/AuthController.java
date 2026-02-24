package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public and authenticated endpoints for the auth subsystem.
 *
 * <ul>
 *   <li>{@code GET /api/auth/me} — returns the currently authenticated user's profile.
 *       Requires a valid JWT in the {@code Authorization} header.</li>
 * </ul>
 *
 * <p>The OAuth2 code-exchange flow is handled by Spring Security's OAuth2 client
 * support; the JWT is issued by {@link OAuth2SuccessHandler} after a successful
 * redirect. A separate {@code POST /api/auth/google} endpoint is provided for
 * headless clients or SPAs that manage the OAuth2 PKCE flow themselves and need to
 * exchange a Google authorization code server-side.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LogManager.getLogger(AuthController.class);

    private final UserRepository   userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider  = tokenProvider;
    }

    // -------------------------------------------------------------------------
    // GET /api/auth/me
    // -------------------------------------------------------------------------

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>The {@link UserPrincipal} is injected by Spring Security from the
     * {@link SecurityContextHolder} after the {@link JwtAuthFilter} has validated
     * the Bearer token.
     *
     * @param principal the authenticated user principal
     * @return 200 with a {@link UserProfileResponse}, or 401 if not authenticated
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = principal.getUser();
        log.debug("GET /api/auth/me for userId={}", user.getId());

        return ResponseEntity.ok(UserProfileResponse.from(user));
    }

    // -------------------------------------------------------------------------
    // Response DTO
    // -------------------------------------------------------------------------

    /**
     * Immutable view of a user's profile returned by {@code /api/auth/me}.
     * Declared as a nested record to keep the auth package self-contained.
     */
    public record UserProfileResponse(
            UUID            id,
            String          email,
            String          displayName,
            String          avatarUrl,
            String          role,
            OffsetDateTime  createdAt,
            OffsetDateTime  lastLoginAt
    ) {
        static UserProfileResponse from(User user) {
            return new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getAvatarUrl(),
                    user.getRole().name(),
                    user.getCreatedAt(),
                    user.getLastLoginAt()
            );
        }
    }
}
