package com.tradeintel.normalize.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for updating an existing jargon dictionary entry.
 * All fields are optional; only non-null values will be applied.
 */
public class JargonUpdateRequest {

    @Size(max = 50, message = "Acronym must not exceed 50 characters")
    private String acronym;

    @Size(max = 500, message = "Expansion must not exceed 500 characters")
    private String expansion;

    @Size(max = 200, message = "Industry must not exceed 200 characters")
    private String industry;

    private String contextExample;

    private Double confidence;

    /**
     * When {@code true}, marks the entry as admin-verified so it is included
     * in LLM extraction prompts.
     */
    private Boolean verified;

    public JargonUpdateRequest() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }
}
