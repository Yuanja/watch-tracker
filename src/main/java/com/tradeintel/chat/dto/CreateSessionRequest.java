package com.tradeintel.chat.dto;

/**
 * Request body for creating a new chat session.
 *
 * @param title optional display title for the session
 */
public record CreateSessionRequest(
        String title
) {}
