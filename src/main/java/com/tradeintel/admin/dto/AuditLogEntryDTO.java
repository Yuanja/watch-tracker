package com.tradeintel.admin.dto;

import com.tradeintel.common.entity.AuditLogEntry;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only projection of an {@link AuditLogEntry} entity for the audit log
 * API endpoint consumed by uber_admin users.
 */
public record AuditLogEntryDTO(
        UUID id,
        UUID actorId,
        String actorEmail,
        String action,
        String targetType,
        UUID targetId,
        String oldValues,
        String newValues,
        String ipAddress,
        OffsetDateTime createdAt
) {

    /**
     * Converts an {@link AuditLogEntry} JPA entity to its API representation.
     * The actor association may be {@code null} for system-initiated actions;
     * {@code actorId} and {@code actorEmail} are set to {@code null} in that case.
     *
     * @param entry the entity to convert; must not be {@code null}
     * @return a populated {@link AuditLogEntryDTO}
     */
    public static AuditLogEntryDTO fromEntity(AuditLogEntry entry) {
        UUID actorId = entry.getActor() != null ? entry.getActor().getId() : null;
        String actorEmail = entry.getActor() != null ? entry.getActor().getEmail() : null;

        return new AuditLogEntryDTO(
                entry.getId(),
                actorId,
                actorEmail,
                entry.getAction(),
                entry.getTargetType(),
                entry.getTargetId(),
                entry.getOldValues(),
                entry.getNewValues(),
                entry.getIpAddress(),
                entry.getCreatedAt()
        );
    }
}
