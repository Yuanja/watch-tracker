package com.tradeintel.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A self-improving dictionary entry mapping a trade-industry acronym or jargon
 * term to its canonical expansion.
 *
 * <p>Entries can originate from the LLM pipeline ({@code source = "llm"}),
 * from seed data ({@code source = "seed"}), or be entered directly by a human
 * ({@code source = "human"}). Entries with {@code verified = false} are queued for
 * admin review before they are included in extraction prompts.</p>
 *
 * Maps to the {@code jargon_dictionary} table with a unique constraint on
 * {@code (acronym, expansion)}.
 */
@Entity
@Table(
    name = "jargon_dictionary",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_jargon_acronym_expansion",
        columnNames = {"acronym", "expansion"}
    )
)
public class JargonEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The short-form term as it appears in trade messages, e.g. "NOS". */
    @Column(name = "acronym", nullable = false)
    private String acronym;

    /** The expanded, human-readable meaning, e.g. "New Old Stock". */
    @Column(name = "expansion", nullable = false)
    private String expansion;

    /** Industry domain this term belongs to, e.g. "industrial surplus". */
    @Column(name = "industry")
    private String industry;

    /** An example message fragment showing the term in context. */
    @Column(name = "context_example", columnDefinition = "text")
    private String contextExample;

    /**
     * How the entry was created.
     * One of: {@code llm} (LLM auto-discovered), {@code human} (admin-entered),
     * {@code seed} (loaded from seed data).
     */
    @Column(name = "source")
    private String source = "llm";

    /** Confidence that this expansion is correct, 0.0 – 1.0. */
    @Column(name = "confidence")
    private Double confidence = 0.5;

    /** Number of times this term has been observed in the archive. */
    @Column(name = "usage_count")
    private Integer usageCount = 1;

    /** Whether an admin has verified this entry for use in extraction prompts. */
    @Column(name = "verified")
    private Boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public JargonEntry() {
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

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getExpansion() {
        return expansion;
    }

    public void setExpansion(String expansion) {
        this.expansion = expansion;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getContextExample() {
        return contextExample;
    }

    public void setContextExample(String contextExample) {
        this.contextExample = contextExample;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
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
