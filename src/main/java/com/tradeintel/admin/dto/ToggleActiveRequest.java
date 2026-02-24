package com.tradeintel.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for the PUT /api/admin/users/{id}/active endpoint.
 * Setting {@code active} to {@code false} immediately prevents the user from
 * authenticating; setting it to {@code true} restores access.
 */
public record ToggleActiveRequest(
        @NotNull(message = "active must not be null")
        Boolean active
) {
}
