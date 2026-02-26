package com.tradeintel.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for PUT /api/admin/groups/{id} — updates mutable settings of a
 * monitored WhatsApp group.
 *
 * <p>The {@code whapiGroupId} is not updatable because it is the stable external
 * key from Whapi.cloud. All other display fields may be changed by the uber_admin.
 */
public record UpdateGroupRequest(
        @NotBlank(message = "groupName must not be blank")
        String groupName,

        String description,

        String avatarUrl,

        Boolean isActive
) {
}
