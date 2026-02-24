package com.tradeintel.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregated cost summary for a single user over a requested date range.
 * Surfaced by the uber_admin cost report endpoints.
 */
public record UserCostSummaryDTO(
        UUID userId,
        String userEmail,
        String displayName,
        long totalInputTokens,
        long totalOutputTokens,
        BigDecimal totalCostUsd,
        int totalSessions,
        int recordCount
) {
}
