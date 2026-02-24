package com.tradeintel.common.entity;

/**
 * Represents the intent of a trade listing extracted from a WhatsApp message.
 * Stored as VARCHAR in JPA (via EnumType.STRING) rather than as a PostgreSQL
 * custom enum type, which simplifies H2-based testing while the Flyway migration
 * defines the actual PG enum for production.
 */
public enum IntentType {
    sell,
    want,
    unknown
}
