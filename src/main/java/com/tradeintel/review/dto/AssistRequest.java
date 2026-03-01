package com.tradeintel.review.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the agent-assisted review endpoint.
 * Contains the admin's natural-language hint to guide re-extraction.
 */
public class AssistRequest {

    @NotBlank(message = "Hint must not be blank")
    private String hint;

    public AssistRequest() {
    }

    public AssistRequest(String hint) {
        this.hint = hint;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
