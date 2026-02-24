package com.tradeintel.listing.dto;

import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.ListingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request body for updating an extracted listing.
 *
 * <p>All fields are optional (null means "do not change"). Only admins and above
 * may submit this payload — the controller enforces this via {@code @AdminOnly}.</p>
 */
public class ListingUpdateRequest {

    private IntentType intent;

    @Size(max = 5000, message = "Item description must not exceed 5000 characters")
    private String itemDescription;

    private UUID itemCategoryId;

    private UUID manufacturerId;

    @Size(max = 200, message = "Part number must not exceed 200 characters")
    private String partNumber;

    @DecimalMin(value = "0", message = "Quantity must be non-negative")
    private BigDecimal quantity;

    private UUID unitId;

    @DecimalMin(value = "0", message = "Price must be non-negative")
    private BigDecimal price;

    @Size(max = 10, message = "Currency code must not exceed 10 characters")
    private String priceCurrency;

    private UUID conditionId;

    private ListingStatus status;

    private Boolean needsHumanReview;

    private OffsetDateTime expiresAt;

    public ListingUpdateRequest() {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public IntentType getIntent() {
        return intent;
    }

    public void setIntent(IntentType intent) {
        this.intent = intent;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public UUID getItemCategoryId() {
        return itemCategoryId;
    }

    public void setItemCategoryId(UUID itemCategoryId) {
        this.itemCategoryId = itemCategoryId;
    }

    public UUID getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(UUID manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public void setUnitId(UUID unitId) {
        this.unitId = unitId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public UUID getConditionId() {
        return conditionId;
    }

    public void setConditionId(UUID conditionId) {
        this.conditionId = conditionId;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public Boolean getNeedsHumanReview() {
        return needsHumanReview;
    }

    public void setNeedsHumanReview(Boolean needsHumanReview) {
        this.needsHumanReview = needsHumanReview;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
