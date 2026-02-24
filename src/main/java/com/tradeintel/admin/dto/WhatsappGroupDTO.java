package com.tradeintel.admin.dto;

import com.tradeintel.common.entity.WhatsappGroup;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@link WhatsappGroup} entity for the admin groups
 * management API. Includes the internal UUID alongside the Whapi group identifier.
 */
public record WhatsappGroupDTO(
        UUID id,
        String whapiGroupId,
        String groupName,
        String description,
        String avatarUrl,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Converts a {@link WhatsappGroup} JPA entity to its API representation.
     *
     * @param group the entity to convert; must not be {@code null}
     * @return a populated {@link WhatsappGroupDTO}
     */
    public static WhatsappGroupDTO fromEntity(WhatsappGroup group) {
        return new WhatsappGroupDTO(
                group.getId(),
                group.getWhapiGroupId(),
                group.getGroupName(),
                group.getDescription(),
                group.getAvatarUrl(),
                group.getIsActive(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
