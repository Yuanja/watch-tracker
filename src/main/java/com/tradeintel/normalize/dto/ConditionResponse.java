package com.tradeintel.normalize.dto;

import com.tradeintel.common.entity.Condition;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only view of a {@code Condition} entity returned by the normalize API.
 */
public class ConditionResponse {

    private UUID id;
    private String name;
    private String abbreviation;
    private Integer sortOrder;
    private Boolean isActive;
    private OffsetDateTime createdAt;

    /**
     * Converts a {@link Condition} JPA entity to a {@code ConditionResponse} DTO.
     *
     * @param condition the entity to convert
     * @return populated response DTO
     */
    public static ConditionResponse fromEntity(Condition condition) {
        ConditionResponse dto = new ConditionResponse();
        dto.setId(condition.getId());
        dto.setName(condition.getName());
        dto.setAbbreviation(condition.getAbbreviation());
        dto.setSortOrder(condition.getSortOrder());
        dto.setIsActive(condition.getIsActive());
        dto.setCreatedAt(condition.getCreatedAt());
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
