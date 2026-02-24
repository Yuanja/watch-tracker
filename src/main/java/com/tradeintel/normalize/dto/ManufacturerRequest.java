package com.tradeintel.normalize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a {@code Manufacturer}.
 */
public class ManufacturerRequest {

    @NotBlank(message = "Manufacturer name must not be blank")
    @Size(max = 200, message = "Manufacturer name must not exceed 200 characters")
    private String name;

    /**
     * Alternative names or abbreviations used in trade messages, e.g. ["Parker", "PH"].
     * May be {@code null} or empty when no aliases are known.
     */
    private String[] aliases;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String website;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

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
}
