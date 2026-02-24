package com.tradeintel.chat.dto;

import com.tradeintel.common.entity.ChatMessage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only response DTO for a {@link ChatMessage} entity.
 *
 * @param id           the message UUID
 * @param role         the chat role (user, assistant, system)
 * @param content      the message content
 * @param modelUsed    the OpenAI model that generated this message (null for user messages)
 * @param inputTokens  token count for the input prompt
 * @param outputTokens token count for the model output
 * @param costUsd      estimated cost in USD for this message
 * @param toolCalls    JSON string of tool call invocations (null when no tools called)
 * @param createdAt    when the message was created
 */
public record ChatMessageDTO(
        UUID id,
        String role,
        String content,
        String modelUsed,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal costUsd,
        String toolCalls,
        OffsetDateTime createdAt
) {
    /**
     * Maps a {@link ChatMessage} entity to its DTO representation.
     *
     * @param msg the entity to map; must not be null
     * @return the populated DTO
     */
    public static ChatMessageDTO fromEntity(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getRole(),
                msg.getContent(),
                msg.getModelUsed(),
                msg.getInputTokens(),
                msg.getOutputTokens(),
                msg.getCostUsd(),
                msg.getToolCalls(),
                msg.getCreatedAt()
        );
    }
}
