package com.tradeintel.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable record of an administrative action taken on any platform entity.
 * Used for compliance auditing and troubleshooting by uber_admin users.
 *
 * <p>The {@code oldValues} and {@code newValues} fields capture the entity state
 * before and after the action as raw JSON, stored in JSONB columns. They are
 * surfaced as {@code String} here; callers use Jackson for (de)serialisation.</p>
 *
 * <p>Rows in this table are never updated or deleted — they form an append-only
 * audit trail. {@code @CreationTimestamp} is the only lifecycle annotation used.</p>
 *
 * Maps to the {@code audit_log} table.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who performed the action.
     * May be {@code null} for system-initiated actions (e.g. pipeline auto-accept).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Short, machine-readable description of the action, e.g. "listing.delete". */
    @Column(name = "action", nullable = false)
    private String action;

    /** The entity type that was affected, e.g. "Listing", "User", "Category". */
    @Column(name = "target_type")
    private String targetType;

    /** The UUID of the specific entity that was affected. */
    @Column(name = "target_id")
    private UUID targetId;

    /**
     * JSON snapshot of the entity's state before the action was applied.
     * {@code null} for create operations.
     * Stored as JSONB; callers use Jackson for (de)serialisation.
     */
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    /**
     * JSON snapshot of the entity's state after the action was applied.
     * {@code null} for delete operations.
     * Stored as JSONB; callers use Jackson for (de)serialisation.
     */
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    /** IP address of the HTTP client that triggered the action, if available. */
    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AuditLogEntry() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
