package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Extends Spring's {@link DefaultOAuth2UserService} to upsert a platform
 * {@link User} record on every successful Google OAuth2 login.
 *
 * <p>On first login a new user is created with the default {@link UserRole#user} role.
 * On subsequent logins the display name, avatar URL, and last-login timestamp are
 * refreshed from the Google profile, but the role and active flag are preserved.
 *
 * <p>The returned {@link OAuth2User} is wrapped in a {@link UserPrincipal} so that
 * downstream Spring Security components (including the
 * {@link OAuth2SuccessHandler}) can access the platform user entity directly.
 */
@Service
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LogManager.getLogger(GoogleOAuth2UserService.class);

    // Google OAuth2 attribute keys
    private static final String ATTR_SUB         = "sub";
    private static final String ATTR_EMAIL        = "email";
    private static final String ATTR_NAME         = "name";
    private static final String ATTR_PICTURE      = "picture";

    private final UserRepository userRepository;

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the OAuth2 user attributes from Google and upserts the corresponding
     * platform user record.
     *
     * @param userRequest the OAuth2 user request containing the access token
     * @return a {@link UserPrincipal} wrapping the persisted {@link User} entity
     * @throws OAuth2AuthenticationException if attribute retrieval or persistence fails
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String googleId    = oAuth2User.getAttribute(ATTR_SUB);
        String email       = oAuth2User.getAttribute(ATTR_EMAIL);
        String displayName = oAuth2User.getAttribute(ATTR_NAME);
        String avatarUrl   = oAuth2User.getAttribute(ATTR_PICTURE);

        log.debug("Processing OAuth2 login for email={}", email);

        User user = upsertUser(googleId, email, displayName, avatarUrl);

        log.info("User login successful: id={} email={} role={}", user.getId(), email,
                user.getRole());

        return UserPrincipal.from(user);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User upsertUser(String googleId, String email, String displayName, String avatarUrl) {
        Optional<User> existing = userRepository.findByGoogleId(googleId);

        if (existing.isPresent()) {
            return updateExistingUser(existing.get(), displayName, avatarUrl);
        }

        // Fallback: look up by email to handle accounts whose googleId was not
        // captured in an earlier version of the application.
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setGoogleId(googleId);
            return updateExistingUser(user, displayName, avatarUrl);
        }

        return createNewUser(googleId, email, displayName, avatarUrl);
    }

    private User updateExistingUser(User user, String displayName, String avatarUrl) {
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setLastLoginAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    private User createNewUser(String googleId, String email, String displayName,
                               String avatarUrl) {
        log.info("Creating new platform user for email={}", email);
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setRole(UserRole.user);
        user.setIsActive(true);
        user.setLastLoginAt(OffsetDateTime.now());
        return userRepository.save(user);
    }
}
