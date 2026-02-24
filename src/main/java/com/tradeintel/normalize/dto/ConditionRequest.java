package com.tradeintel.normalize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a {@code Condition}.
 */
public class ConditionRequest {

    @NotBlank(message = "Condition name must not be blank")
    @Size(max = 200, message = "Condition name must not exceed 200 characters")
    private String name;

    @Size(max = 20, message = "Condition abbreviation must not exceed 20 characters")
    private String abbreviation;

    private Integer sortOrder;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
