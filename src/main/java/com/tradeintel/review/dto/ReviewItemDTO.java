package com.tradeintel.review.dto;

import com.tradeintel.common.entity.ReviewQueueItem;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data transfer object for review queue items surfaced by the admin review API.
 *
 * <p>Contains all fields needed by the admin UI to display the review item,
 * including the original message content and LLM extraction suggestions.</p>
 */
public class ReviewItemDTO {

    private UUID id;
    private UUID listingId;
    private UUID rawMessageId;
    private String reason;
    private String llmExplanation;
    private String suggestedValues;
    private String status;
    private String originalMessageBody;
    private String senderName;
    private OffsetDateTime createdAt;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates a DTO from a {@link ReviewQueueItem} entity.
     *
     * @param item the entity to convert
     * @return the populated DTO
     */
    public static ReviewItemDTO fromEntity(ReviewQueueItem item) {
        ReviewItemDTO dto = new ReviewItemDTO();
        dto.setId(item.getId());
        dto.setListingId(item.getListing() != null ? item.getListing().getId() : null);
        dto.setRawMessageId(item.getRawMessage() != null ? item.getRawMessage().getId() : null);
        dto.setReason(item.getReason());
        dto.setLlmExplanation(item.getLlmExplanation());
        dto.setSuggestedValues(item.getSuggestedValues());
        dto.setStatus(item.getStatus());
        dto.setCreatedAt(item.getCreatedAt());

        // Pull original message content from the raw message
        if (item.getRawMessage() != null) {
            dto.setOriginalMessageBody(item.getRawMessage().getMessageBody());
            dto.setSenderName(item.getRawMessage().getSenderName());
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

    public UUID getListingId() {
        return listingId;
    }

    public void setListingId(UUID listingId) {
        this.listingId = listingId;
    }

    public UUID getRawMessageId() {
        return rawMessageId;
    }

    public void setRawMessageId(UUID rawMessageId) {
        this.rawMessageId = rawMessageId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLlmExplanation() {
        return llmExplanation;
    }

    public void setLlmExplanation(String llmExplanation) {
        this.llmExplanation = llmExplanation;
    }

    public String getSuggestedValues() {
        return suggestedValues;
    }

    public void setSuggestedValues(String suggestedValues) {
        this.suggestedValues = suggestedValues;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalMessageBody() {
        return originalMessageBody;
    }

    public void setOriginalMessageBody(String originalMessageBody) {
        this.originalMessageBody = originalMessageBody;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
