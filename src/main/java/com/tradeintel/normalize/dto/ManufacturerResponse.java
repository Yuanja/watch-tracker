package com.tradeintel.normalize.dto;

import com.tradeintel.common.entity.Manufacturer;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only view of a {@code Manufacturer} entity returned by the normalize API.
 */
public class ManufacturerResponse {

    private UUID id;
    private String name;
    private String[] aliases;
    private String website;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * Converts a {@link Manufacturer} JPA entity to a {@code ManufacturerResponse} DTO.
     *
     * @param manufacturer the entity to convert
     * @return populated response DTO
     */
    public static ManufacturerResponse fromEntity(Manufacturer manufacturer) {
        ManufacturerResponse dto = new ManufacturerResponse();
        dto.setId(manufacturer.getId());
        dto.setName(manufacturer.getName());
        dto.setAliases(manufacturer.getAliases());
        dto.setWebsite(manufacturer.getWebsite());
        dto.setIsActive(manufacturer.getIsActive());
        dto.setCreatedAt(manufacturer.getCreatedAt());
        dto.setUpdatedAt(manufacturer.getUpdatedAt());
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
