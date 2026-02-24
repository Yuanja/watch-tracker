package com.tradeintel.normalize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for creating or updating a {@code Category}.
 */
public class CategoryRequest {

    @NotBlank(message = "Category name must not be blank")
    @Size(max = 200, message = "Category name must not exceed 200 characters")
    private String name;

    /** Optional parent category UUID; {@code null} indicates a root category. */
    private UUID parentId;

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

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
