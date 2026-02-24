package com.tradeintel.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/admin/groups — adds a new WhatsApp group to monitor.
 *
 * <p>Both {@code whapiGroupId} and {@code groupName} are mandatory. The Whapi
 * group identifier is the stable external key assigned by Whapi.cloud and must
 * be unique across all monitored groups.
 */
public record CreateGroupRequest(
        @NotBlank(message = "whapiGroupId must not be blank")
        String whapiGroupId,

        @NotBlank(message = "groupName must not be blank")
        String groupName,

        String description,

        String avatarUrl
) {
}
