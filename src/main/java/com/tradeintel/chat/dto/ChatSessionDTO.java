package com.tradeintel.chat.dto;

import com.tradeintel.common.entity.ChatSession;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only response DTO for a {@link ChatSession} entity (without messages).
 *
 * @param id        the session UUID
 * @param title     the display title for the session
 * @param createdAt when the session was created
 * @param updatedAt when the session was last updated
 */
public record ChatSessionDTO(
        UUID id,
        String title,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * Maps a {@link ChatSession} entity to its DTO representation.
     *
     * @param session the entity to map; must not be null
     * @return the populated DTO
     */
    public static ChatSessionDTO fromEntity(ChatSession session) {
        return new ChatSessionDTO(
                session.getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
