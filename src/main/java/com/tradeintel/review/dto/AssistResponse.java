package com.tradeintel.review.dto;

import com.tradeintel.processing.ExtractionResult;

/**
 * Response DTO for the agent-assisted review endpoint.
 * Contains the refined extraction result and the original message text for context.
 */
public class AssistResponse {

    private ExtractionResult extraction;
    private String originalText;

    public AssistResponse() {
    }

    public AssistResponse(ExtractionResult extraction, String originalText) {
        this.extraction = extraction;
        this.originalText = originalText;
    }

    public ExtractionResult getExtraction() {
        return extraction;
    }

    public void setExtraction(ExtractionResult extraction) {
        this.extraction = extraction;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
}
