package com.tradeintel.chat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for a user's accumulated cost summary including daily breakdown.
 *
 * @param totalInputTokens  total input tokens across all days
 * @param totalOutputTokens total output tokens across all days
 * @param totalCostUsd      total cost in USD across all days
 * @param sessionCount      total number of sessions across all days
 * @param dailyBreakdown    list of daily usage entries
 */
public record UserCostDTO(
        long totalInputTokens,
        long totalOutputTokens,
        BigDecimal totalCostUsd,
        int sessionCount,
        List<DailyUsage> dailyBreakdown
) {
    /**
     * A single day's usage record.
     *
     * @param date         the calendar date
     * @param inputTokens  input tokens used on this day
     * @param outputTokens output tokens used on this day
     * @param costUsd      cost in USD for this day
     */
    public record DailyUsage(
            LocalDate date,
            long inputTokens,
            long outputTokens,
            BigDecimal costUsd
    ) {}
}
