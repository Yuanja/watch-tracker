package com.tradeintel.normalize.dto;

import com.tradeintel.common.entity.Unit;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only view of a {@code Unit} entity returned by the normalize API.
 */
public class UnitResponse {

    private UUID id;
    private String name;
    private String abbreviation;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    /**
     * Converts a {@link Unit} JPA entity to a {@code UnitResponse} DTO.
     *
     * @param unit the entity to convert
     * @return populated response DTO
     */
    public static UnitResponse fromEntity(Unit unit) {
        UnitResponse dto = new UnitResponse();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setAbbreviation(unit.getAbbreviation());
        dto.setIsActive(unit.getIsActive());
        dto.setCreatedAt(unit.getCreatedAt());
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

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
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
}
