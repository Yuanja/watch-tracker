package com.tradeintel.common.entity;

/**
 * Three-tier role system controlling access to platform features.
 * Stored as VARCHAR in JPA (via EnumType.STRING) rather than as a PostgreSQL
 * custom enum type, which simplifies H2-based testing while the Flyway migration
 * defines the actual PG enum for production.
 *
 * <ul>
 *   <li>user      — replay, chat, listings, notifications, personal cost view</li>
 *   <li>admin     — above + review queue, normalized value CRUD, jargon management</li>
 *   <li>uber_admin — above + user management, all chats view, cost reports, audit log,
 *                   WhatsApp group management</li>
 * </ul>
 */
public enum UserRole {
    user,
    admin,
    uber_admin
}
