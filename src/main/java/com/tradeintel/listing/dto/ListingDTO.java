package com.tradeintel.listing.dto;

import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-only response DTO for a {@link Listing} entity.
 *
 * <p>Joins related entity names (category, manufacturer, unit, condition) so that
 * API consumers receive denormalised display values without needing secondary
 * lookups. The raw embedding float array is intentionally excluded.</p>
 */
public class ListingDTO {

    private UUID id;

    // Provenance
    private UUID rawMessageId;
    private UUID groupId;
    private String groupName;

    // Classification
    private IntentType intent;
    private Double confidenceScore;

    // Normalised fields
    private String itemDescription;
    private UUID itemCategoryId;
    private String itemCategoryName;
    private UUID manufacturerId;
    private String manufacturerName;
    private String partNumber;
    private BigDecimal quantity;
    private UUID unitId;
    private String unitName;
    private String unitAbbreviation;
    private BigDecimal price;
    private String priceCurrency;
    private BigDecimal exchangeRateToUsd;
    private BigDecimal priceUsd;
    private int crossPostCount;
    private UUID conditionId;
    private String conditionName;

    // Original content
    private String originalText;
    private String senderName;
    private String senderPhone;

    // Review state
    private ListingStatus status;
    private Boolean needsHumanReview;
    private UUID reviewedById;
    private String reviewedByName;
    private OffsetDateTime reviewedAt;

    // Sold tracking
    private OffsetDateTime soldAt;
    private String soldMessageId;
    private String buyerName;

    // Message timestamp (when the WhatsApp message was originally sent)
    private OffsetDateTime messageTimestamp;

    // Lifecycle
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime deletedAt;
    private UUID deletedById;

    public ListingDTO() {
    }

    /**
     * Maps a {@link Listing} entity to its DTO representation.
     * Lazy-loaded associations are accessed safely; null associations produce
     * null ID and name fields in the DTO.
     *
     * @param listing the entity to map; must not be null
     * @return the populated DTO
     */
    public static ListingDTO fromEntity(Listing listing) {
        ListingDTO dto = new ListingDTO();

        dto.id = listing.getId();

        // Provenance
        if (listing.getRawMessage() != null) {
            dto.rawMessageId = listing.getRawMessage().getId();
        }
        if (listing.getGroup() != null) {
            dto.groupId = listing.getGroup().getId();
            dto.groupName = listing.getGroup().getGroupName();
        }

        // Classification
        dto.intent = listing.getIntent();
        dto.confidenceScore = listing.getConfidenceScore();

        // Normalised fields
        dto.itemDescription = listing.getItemDescription();
        if (listing.getItemCategory() != null) {
            dto.itemCategoryId = listing.getItemCategory().getId();
            dto.itemCategoryName = listing.getItemCategory().getName();
        }
        if (listing.getManufacturer() != null) {
            dto.manufacturerId = listing.getManufacturer().getId();
            dto.manufacturerName = listing.getManufacturer().getName();
        }
        dto.partNumber = listing.getPartNumber();
        dto.quantity = listing.getQuantity();
        if (listing.getUnit() != null) {
            dto.unitId = listing.getUnit().getId();
            dto.unitName = listing.getUnit().getName();
            dto.unitAbbreviation = listing.getUnit().getAbbreviation();
        }
        dto.price = listing.getPrice();
        dto.priceCurrency = listing.getPriceCurrency();
        dto.exchangeRateToUsd = listing.getExchangeRateToUsd();
        dto.priceUsd = listing.getPriceUsd();
        if (listing.getCondition() != null) {
            dto.conditionId = listing.getCondition().getId();
            dto.conditionName = listing.getCondition().getName();
        }

        // Original content
        dto.originalText = listing.getOriginalText();
        dto.senderName = listing.getSenderName();
        dto.senderPhone = listing.getSenderPhone();

        // Review state
        dto.status = listing.getStatus();
        dto.needsHumanReview = listing.getNeedsHumanReview();
        if (listing.getReviewedBy() != null) {
            dto.reviewedById = listing.getReviewedBy().getId();
            dto.reviewedByName = listing.getReviewedBy().getDisplayName();
        }
        dto.reviewedAt = listing.getReviewedAt();

        // Sold tracking
        dto.soldAt = listing.getSoldAt();
        dto.soldMessageId = listing.getSoldMessageId();
        dto.buyerName = listing.getBuyerName();

        // Message timestamp
        if (listing.getRawMessage() != null) {
            dto.messageTimestamp = listing.getRawMessage().getTimestampWa();
        }

        // Lifecycle
        dto.createdAt = listing.getCreatedAt();
        dto.updatedAt = listing.getUpdatedAt();
        dto.expiresAt = listing.getExpiresAt();
        dto.deletedAt = listing.getDeletedAt();
        if (listing.getDeletedBy() != null) {
            dto.deletedById = listing.getDeletedBy().getId();
        }

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

    public UUID getRawMessageId() {
        return rawMessageId;
    }

    public void setRawMessageId(UUID rawMessageId) {
        this.rawMessageId = rawMessageId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public IntentType getIntent() {
        return intent;
    }

    public void setIntent(IntentType intent) {
        this.intent = intent;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
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

    public String getItemCategoryName() {
        return itemCategoryName;
    }

    public void setItemCategoryName(String itemCategoryName) {
        this.itemCategoryName = itemCategoryName;
    }

    public UUID getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(UUID manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
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

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitAbbreviation() {
        return unitAbbreviation;
    }

    public void setUnitAbbreviation(String unitAbbreviation) {
        this.unitAbbreviation = unitAbbreviation;
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

    public BigDecimal getExchangeRateToUsd() {
        return exchangeRateToUsd;
    }

    public void setExchangeRateToUsd(BigDecimal exchangeRateToUsd) {
        this.exchangeRateToUsd = exchangeRateToUsd;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public void setPriceUsd(BigDecimal priceUsd) {
        this.priceUsd = priceUsd;
    }

    public int getCrossPostCount() {
        return crossPostCount;
    }

    public void setCrossPostCount(int crossPostCount) {
        this.crossPostCount = crossPostCount;
    }

    public UUID getConditionId() {
        return conditionId;
    }

    public void setConditionId(UUID conditionId) {
        this.conditionId = conditionId;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
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

    public UUID getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(UUID reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public OffsetDateTime getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(OffsetDateTime messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
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

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedById() {
        return deletedById;
    }

    public void setDeletedById(UUID deletedById) {
        this.deletedById = deletedById;
    }

    public OffsetDateTime getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(OffsetDateTime soldAt) {
        this.soldAt = soldAt;
    }

    public String getSoldMessageId() {
        return soldMessageId;
    }

    public void setSoldMessageId(String soldMessageId) {
        this.soldMessageId = soldMessageId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }
}
