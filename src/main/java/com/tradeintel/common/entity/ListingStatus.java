package com.tradeintel.common.entity;

/**
 * Lifecycle status of an extracted listing.
 * Stored as VARCHAR in JPA (via EnumType.STRING) rather than as a PostgreSQL
 * custom enum type, which simplifies H2-based testing while the Flyway migration
 * defines the actual PG enum for production.
 */
public enum ListingStatus {
    active,
    expired,
    deleted,
    pending_review,
    sold
}
