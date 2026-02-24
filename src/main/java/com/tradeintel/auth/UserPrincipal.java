package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring Security principal that implements both {@link UserDetails} (for JWT
 * filter authentication) and {@link OAuth2User} (for Google OAuth2 login flow).
 *
 * <p>Implementing both interfaces means that a single class is returned from
 * {@link GoogleOAuth2UserService#loadUser} and can also be injected via
 * {@code @AuthenticationPrincipal} in controllers regardless of the authentication
 * mechanism used.
 *
 * <p>Authority mapping follows the cumulative role hierarchy:
 * <ul>
 *   <li>{@code user}       → {@code ROLE_USER}</li>
 *   <li>{@code admin}      → {@code ROLE_USER, ROLE_ADMIN}</li>
 *   <li>{@code uber_admin} → {@code ROLE_USER, ROLE_ADMIN, ROLE_UBER_ADMIN}</li>
 * </ul>
 *
 * <p>The username exposed to Spring Security is the user's email address.
 */
public class UserPrincipal implements UserDetails, OAuth2User {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.user = user;
        this.authorities = buildAuthorities(user.getRole());
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    // -------------------------------------------------------------------------
    // Domain accessors
    // -------------------------------------------------------------------------

    /** Returns the underlying JPA entity. */
    public User getUser() {
        return user;
    }

    /** Convenience accessor for the user's UUID primary key. */
    public UUID getUserId() {
        return user.getId();
    }

    // -------------------------------------------------------------------------
    // UserDetails implementation
    // -------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Returns {@code null} — this application uses JWT tokens for authentication,
     * not password-based login. Passwords are never stored.
     */
    @Override
    public String getPassword() {
        return null;
    }

    /** Uses the user's email as the Spring Security principal name. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(user.getIsActive());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }

    // -------------------------------------------------------------------------
    // OAuth2User implementation
    // -------------------------------------------------------------------------

    /**
     * Returns an empty map because the OAuth2 attributes are not stored on this
     * principal after the initial login and upsert. All relevant user data is
     * available through the wrapped {@link User} entity.
     *
     * <p>Callers that need the raw Google profile attributes (e.g. the picture URL)
     * should access them via {@link #getUser()}.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    /**
     * Returns the email address as the OAuth2 principal name, consistent with
     * the value returned by {@link #getUsername()}.
     */
    @Override
    public String getName() {
        return user.getEmail();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the set of {@link GrantedAuthority} instances for the given role.
     * Higher roles inherit all authorities from lower roles.
     */
    private static List<SimpleGrantedAuthority> buildAuthorities(UserRole role) {
        return switch (role) {
            case uber_admin -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_UBER_ADMIN")
            );
            case admin -> List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            default -> List.of(new SimpleGrantedAuthority("ROLE_USER"));
        };
    }
}
