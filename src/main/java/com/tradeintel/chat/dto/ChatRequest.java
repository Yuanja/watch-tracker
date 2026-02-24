package com.tradeintel.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for sending a user message in a chat session.
 *
 * @param message the user's message text
 */
public record ChatRequest(
        @NotBlank(message = "message is required")
        String message
) {}
