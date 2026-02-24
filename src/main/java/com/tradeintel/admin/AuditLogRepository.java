package com.tradeintel.admin;

import com.tradeintel.common.entity.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditLogEntry} entities.
 *
 * <p>Audit log entries are append-only; this repository exposes only read
 * operations and the inherited {@link JpaRepository#save} used during log
 * creation. No update or delete methods are exposed.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    /**
     * Returns a page of audit log entries created by the given actor,
     * newest first.
     *
     * @param actorId  the UUID of the acting user
     * @param pageable pagination and sorting parameters
     * @return page of entries ordered by {@code created_at} descending
     */
    Page<AuditLogEntry> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    /**
     * Returns all audit log entries that affected a specific entity instance,
     * identified by its type name and UUID.
     *
     * @param targetType the entity class name, e.g. "User" or "WhatsappGroup"
     * @param targetId   the UUID of the affected entity
     * @return unordered list of matching entries (typically small)
     */
    List<AuditLogEntry> findByTargetTypeAndTargetId(String targetType, UUID targetId);

    /**
     * Returns a page of all audit log entries ordered by creation time descending.
     * Used for the global audit log view available to uber_admin.
     *
     * @param pageable pagination and sorting parameters
     * @return page of entries ordered by {@code created_at} descending
     */
    Page<AuditLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
