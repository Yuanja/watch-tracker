package com.tradeintel.admin.dto;

import com.tradeintel.common.entity.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for the PUT /api/admin/users/{id}/role endpoint.
 * The {@code role} field must be one of the valid {@link UserRole} values.
 */
public record ChangeRoleRequest(
        @NotNull(message = "role must not be null")
        UserRole role
) {
}
