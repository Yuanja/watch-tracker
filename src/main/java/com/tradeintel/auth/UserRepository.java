package com.tradeintel.auth;

import com.tradeintel.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 * Used by the OAuth2 user service to upsert users on first login and
 * by the JWT filter to load a user principal for authenticated requests.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their stable Google OAuth2 subject identifier.
     * This is the primary lookup during the OAuth2 login flow.
     *
     * @param googleId the {@code sub} claim from the Google ID token
     * @return the matching user if present
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Finds a user by their email address.
     * Used as a fallback when a returning user's Google subject ID is
     * not yet stored (e.g., during data migration).
     *
     * @param email the user's email address
     * @return the matching user if present
     */
    Optional<User> findByEmail(String email);
}
