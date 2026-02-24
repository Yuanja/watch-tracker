package com.tradeintel.normalize.dto;

import com.tradeintel.common.entity.JargonEntry;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only response DTO for a {@link JargonEntry}.
 * Returned by all jargon API endpoints to avoid exposing JPA entities directly.
 */
public class JargonEntryDTO {

    private UUID id;
    private String acronym;
    private String expansion;
    private String industry;
    private String contextExample;
    private String source;
    private Double confidence;
    private Integer usageCount;
    private Boolean verified;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public JargonEntryDTO() {
    }

    /**
     * Maps a {@link JargonEntry} entity to its DTO representation.
     *
     * @param entry the entity to map; must not be null
     * @return the populated DTO
     */
    public static JargonEntryDTO fromEntity(JargonEntry entry) {
        JargonEntryDTO dto = new JargonEntryDTO();
        dto.id = entry.getId();
        dto.acronym = entry.getAcronym();
        dto.expansion = entry.getExpansion();
        dto.industry = entry.getIndustry();
        dto.contextExample = entry.getContextExample();
        dto.source = entry.getSource();
        dto.confidence = entry.getConfidence();
        dto.usageCount = entry.getUsageCount();
        dto.verified = entry.getVerified();
        dto.createdAt = entry.getCreatedAt();
        dto.updatedAt = entry.getUpdatedAt();
        return dto;
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
