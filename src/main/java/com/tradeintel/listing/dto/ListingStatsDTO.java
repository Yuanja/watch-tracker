package com.tradeintel.listing.dto;

import java.util.Map;

/**
 * Aggregated listing statistics returned by {@code GET /api/listings/stats}.
 *
 * <p>Provides counts broken down by intent type and lifecycle status, plus the
 * total number of non-deleted listings. Used by the admin dashboard to give a
 * quick overview of the extraction pipeline's output.</p>
 */
public class ListingStatsDTO {

    /** Total number of non-soft-deleted listings. */
    private long total;

    /**
     * Count per intent value.
     * Keys match {@link com.tradeintel.common.entity.IntentType} names:
     * {@code sell}, {@code want}, {@code unknown}.
     */
    private Map<String, Long> byIntent;

    /**
     * Count per status value.
     * Keys match {@link com.tradeintel.common.entity.ListingStatus} names:
     * {@code active}, {@code expired}, {@code deleted}, {@code pending_review}.
     */
    private Map<String, Long> byStatus;

    public ListingStatsDTO() {
    }

    public ListingStatsDTO(long total, Map<String, Long> byIntent, Map<String, Long> byStatus) {
        this.total = total;
        this.byIntent = byIntent;
        this.byStatus = byStatus;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public Map<String, Long> getByIntent() {
        return byIntent;
    }

    public void setByIntent(Map<String, Long> byIntent) {
        this.byIntent = byIntent;
    }

    public Map<String, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<String, Long> byStatus) {
        this.byStatus = byStatus;
    }
}
