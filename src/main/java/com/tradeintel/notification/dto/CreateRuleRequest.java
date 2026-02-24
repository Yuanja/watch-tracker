package com.tradeintel.notification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a new notification rule from natural language.
 *
 * @param nlRule     the natural language description of the notification rule
 * @param notifyEmail optional email address to send notifications to (defaults to user's email)
 */
public record CreateRuleRequest(
        @NotBlank(message = "nlRule is required")
        String nlRule,
        String notifyEmail
) {}
