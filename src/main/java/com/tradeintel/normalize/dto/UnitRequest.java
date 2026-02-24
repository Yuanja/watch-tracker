package com.tradeintel.normalize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a {@code Unit}.
 */
public class UnitRequest {

    @NotBlank(message = "Unit name must not be blank")
    @Size(max = 100, message = "Unit name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Unit abbreviation must not be blank")
    @Size(max = 20, message = "Unit abbreviation must not exceed 20 characters")
    private String abbreviation;

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
}
