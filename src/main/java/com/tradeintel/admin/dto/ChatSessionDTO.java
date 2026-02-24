package com.tradeintel.admin.dto;

import com.tradeintel.common.entity.ChatSession;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@link ChatSession} entity for use in admin and
 * user-facing chat session listing endpoints.
 */
public record ChatSessionDTO(
        UUID id,
        UUID userId,
        String userEmail,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * Converts a {@link ChatSession} JPA entity to its API representation.
     * Accesses {@code session.getUser()} — the caller must ensure the user
     * association is initialised (not a lazy proxy).
     *
     * @param session the entity to convert; must not be {@code null}
     * @return a populated {@link ChatSessionDTO}
     */
    public static ChatSessionDTO fromEntity(ChatSession session) {
        return new ChatSessionDTO(
                session.getId(),
                session.getUser().getId(),
                session.getUser().getEmail(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
