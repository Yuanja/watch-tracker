package com.tradeintel.chat.dto;

import java.util.List;

/**
 * Response DTO for a chat message exchange containing the assistant's reply
 * and any tool results that were gathered during processing.
 *
 * @param message     the assistant's response message
 * @param toolResults list of tool result summaries (empty when no tools were called)
 */
public record ChatResponseDTO(
        ChatMessageDTO message,
        List<String> toolResults
) {}
