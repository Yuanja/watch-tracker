package com.tradeintel.normalize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new jargon dictionary entry.
 */
public class JargonCreateRequest {

    @NotBlank(message = "Acronym must not be blank")
    @Size(max = 50, message = "Acronym must not exceed 50 characters")
    private String acronym;

    @NotBlank(message = "Expansion must not be blank")
    @Size(max = 500, message = "Expansion must not exceed 500 characters")
    private String expansion;

    @Size(max = 200, message = "Industry must not exceed 200 characters")
    private String industry;

    private String contextExample;

    /**
     * Origin of the entry: {@code human}, {@code seed}, or {@code llm}.
     * Defaults to {@code human} for entries created via the admin API.
     */
    @Size(max = 20, message = "Source must not exceed 20 characters")
    private String source = "human";

    private Double confidence = 1.0;

    public JargonCreateRequest() {
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
}
