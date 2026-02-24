package com.tradeintel.admin.dto;

import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@link User} entity surfaced by the admin API.
 * Does not expose the Google subject identifier or other sensitive internal fields.
 */
public record UserDTO(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        UserRole role,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt
) {

    /**
     * Converts a {@link User} JPA entity to its API representation.
     *
     * @param user the entity to convert; must not be {@code null}
     * @return a populated {@link UserDTO}
     */
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
