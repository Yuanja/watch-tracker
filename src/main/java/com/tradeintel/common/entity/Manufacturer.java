package com.tradeintel.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin-managed manufacturer used to normalise listing provenance.
 * The {@code aliases} field maps to a PostgreSQL {@code TEXT[]} array column
 * and is annotated with {@code @JdbcTypeCode(SqlTypes.ARRAY)} so Hibernate
 * can read and write it correctly against a real PostgreSQL data source.
 * Maps to the {@code manufacturers} table.
 */
@Entity
@Table(name = "manufacturers")
public class Manufacturer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Canonical manufacturer name, e.g. "Parker Hannifin". */
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    /**
     * Alternative names / abbreviations used in trade messages, e.g. ["Parker", "PH"].
     * Stored as {@code TEXT[]} in PostgreSQL; Hibernate maps this via the ARRAY JDBC type.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "aliases", columnDefinition = "text[]")
    private String[] aliases;

    @Column(name = "website")
    private String website;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Manufacturer() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
