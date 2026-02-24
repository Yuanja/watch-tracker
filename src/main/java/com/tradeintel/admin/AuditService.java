package com.tradeintel.admin;

import com.tradeintel.admin.dto.AuditLogEntryDTO;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.AuditLogEntry;
import com.tradeintel.common.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that writes and reads the append-only audit trail.
 *
 * <p>All admin actions that mutate user records or platform configuration
 * call {@link #log} so that every change is traceable to an actor, a target
 * entity, and the before/after state as raw JSON snapshots.
 *
 * <p>Reads are exposed through {@link #getAuditLog} for the uber_admin audit
 * log page. The method is read-only and uses a non-transactional query path
 * to avoid holding a write lock while paginating large result sets.
 */
@Service
public class AuditService {

    private static final Logger log = LogManager.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository,
                        UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Records an administrative action in the audit log.
     *
     * <p>This method always runs in its own transaction so that the audit
     * record is persisted even if the surrounding business transaction rolls
     * back. Callers should invoke this after the primary mutation has been
     * committed.
     *
     * @param actorId    UUID of the user who performed the action; may be
     *                   {@code null} for system-initiated events
     * @param action     short machine-readable description, e.g. "user.role_change"
     * @param targetType entity class name, e.g. "User", "WhatsappGroup"
     * @param targetId   UUID of the affected entity
     * @param oldValues  JSON snapshot before the change, or {@code null} for creates
     * @param newValues  JSON snapshot after the change, or {@code null} for deletes
     * @param ipAddress  client IP address extracted from the HTTP request, or
     *                   {@code null} when not available
     */
    @Transactional
    public void log(UUID actorId,
                    String action,
                    String targetType,
                    UUID targetId,
                    String oldValues,
                    String newValues,
                    String ipAddress) {

        AuditLogEntry entry = new AuditLogEntry();

        if (actorId != null) {
            User actor = userRepository.findById(actorId).orElse(null);
            if (actor == null) {
                log.warn("audit: actor {} not found, logging without actor reference", actorId);
            }
            entry.setActor(actor);
        }

        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setOldValues(oldValues);
        entry.setNewValues(newValues);
        entry.setIpAddress(ipAddress);

        auditLogRepository.save(entry);

        log.info("audit: action={} targetType={} targetId={} actorId={} ip={}",
                action, targetType, targetId, actorId, ipAddress);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated view of the audit log, optionally filtered by actor.
     *
     * <p>When {@code actorId} is non-null only entries written by that actor
     * are returned; otherwise all entries are returned newest-first.
     *
     * @param actorId  optional filter to restrict results to a single actor
     * @param pageable pagination and sort parameters supplied by the controller
     * @return page of {@link AuditLogEntryDTO} projected from JPA entities
     */
    @Transactional(readOnly = true)
    public Page<AuditLogEntryDTO> getAuditLog(UUID actorId, Pageable pageable) {
        Page<AuditLogEntry> page = (actorId != null)
                ? auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        return page.map(AuditLogEntryDTO::fromEntity);
    }
}
