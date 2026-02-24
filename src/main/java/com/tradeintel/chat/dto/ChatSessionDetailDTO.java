package com.tradeintel.chat.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a chat session including its full message history.
 *
 * @param id        the session UUID
 * @param title     the display title for the session
 * @param messages  all messages in the session in chronological order
 * @param createdAt when the session was created
 */
public record ChatSessionDetailDTO(
        UUID id,
        String title,
        List<ChatMessageDTO> messages,
        OffsetDateTime createdAt
) {}
