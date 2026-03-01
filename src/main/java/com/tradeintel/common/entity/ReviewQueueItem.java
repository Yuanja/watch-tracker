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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A listing that the automated confidence routing has flagged for human review.
 *
 * <p>Listings with a confidence score between 0.5 and 0.8 are placed in this queue
 * rather than being auto-accepted or discarded. An admin can then approve, edit,
 * or skip the item.</p>
 *
 * <p>The {@code suggestedValues} and {@code resolution} fields store arbitrary JSON
 * objects as JSONB columns. They are surfaced as {@code String} here; callers use
 * Jackson to serialise/deserialise before use.</p>
 *
 * Maps to the {@code review_queue} table.
 */
@Entity
@Table(name = "review_queue")
public class ReviewQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The (possibly partial) listing created for this message, if auto-accept
     * was not triggered. May be {@code null} if extraction confidence was too low
     * to create any listing record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    /** The original WhatsApp message that triggered the review. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_message_id", nullable = false)
    private RawMessage rawMessage;

    /** Human-readable explanation of why this item was flagged for review. */
    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    /** The LLM's own explanation of its uncertainty or the extraction problem. */
    @Column(name = "llm_explanation", columnDefinition = "text")
    private String llmExplanation;

    /**
     * JSON object containing the LLM's suggested field values for the listing.
     * The admin can choose to accept, modify, or ignore these suggestions.
     * Stored as JSONB; callers use Jackson for (de)serialisation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_values", columnDefinition = "jsonb")
    private String suggestedValues;

    /**
     * Review state. One of: {@code pending}, {@code resolved}, {@code skipped}.
     * Enforced by a CHECK constraint in the Flyway migration.
     */
    @Column(name = "status")
    private String status = "pending";

    /** The admin user who resolved or skipped this review item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    /**
     * JSON object capturing the admin's resolution (approved fields, rejection reason, etc.).
     * Stored as JSONB; callers use Jackson for (de)serialisation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolution", columnDefinition = "jsonb")
    private String resolution;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ReviewQueueItem() {
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

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public RawMessage getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(RawMessage rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLlmExplanation() {
        return llmExplanation;
    }

    public void setLlmExplanation(String llmExplanation) {
        this.llmExplanation = llmExplanation;
    }

    public String getSuggestedValues() {
        return suggestedValues;
    }

    public void setSuggestedValues(String suggestedValues) {
        this.suggestedValues = suggestedValues;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(User resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
