package com.tradeintel.listing.dto;

import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Compact DTO for cross-posted listings (same item posted across groups or re-posted over time).
 */
public class CrossPostDTO {

    private UUID id;
    private String groupName;
    private String senderName;
    private BigDecimal price;
    private String priceCurrency;
    private ListingStatus status;
    private OffsetDateTime messageTimestamp;
    private OffsetDateTime createdAt;

    public CrossPostDTO() {
    }

    public static CrossPostDTO fromEntity(Listing listing) {
        CrossPostDTO dto = new CrossPostDTO();
        dto.id = listing.getId();
        if (listing.getGroup() != null) {
            dto.groupName = listing.getGroup().getGroupName();
        }
        dto.senderName = listing.getSenderName();
        dto.price = listing.getPrice();
        dto.priceCurrency = listing.getPriceCurrency();
        dto.status = listing.getStatus();
        if (listing.getRawMessage() != null) {
            dto.messageTimestamp = listing.getRawMessage().getTimestampWa();
        }
        dto.createdAt = listing.getCreatedAt();
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
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

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
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
}
