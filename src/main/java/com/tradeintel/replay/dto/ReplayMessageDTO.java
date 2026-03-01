package com.tradeintel.replay.dto;

import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.RawMessage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReplayMessageDTO {

    private UUID id;
    private UUID groupId;
    private String groupName;
    private String whapiMsgId;
    private String senderPhone;
    private String senderName;
    private String senderAvatar;
    private String messageBody;
    private String messageType;
    private String mediaUrl;
    private String mediaMimeType;
    private String mediaLocalPath;
    private String replyToMsgId;
    private boolean isForwarded;
    private OffsetDateTime timestampWa;
    private boolean processed;
    private String rawJson;
    private ExtractedListingRef extractedListing;

    public ReplayMessageDTO() {}

    public static class ExtractedListingRef {
        private UUID id;
        private String intent;
        private String itemDescription;
        private double confidenceScore;
        private String partNumber;
        private String manufacturerName;
        private BigDecimal price;
        private String conditionName;
        private String status;
        private String soldAt;
        private String buyerName;

        public static ExtractedListingRef fromListing(Listing listing) {
            ExtractedListingRef ref = new ExtractedListingRef();
            ref.setId(listing.getId());
            ref.setIntent(listing.getIntent() != null ? listing.getIntent().name() : "unknown");
            ref.setItemDescription(listing.getItemDescription());
            ref.setConfidenceScore(listing.getConfidenceScore() != null ? listing.getConfidenceScore() : 0.0);
            ref.setPartNumber(listing.getPartNumber());
            ref.setManufacturerName(listing.getManufacturer() != null ? listing.getManufacturer().getName() : null);
            ref.setPrice(listing.getPrice());
            ref.setConditionName(listing.getCondition() != null ? listing.getCondition().getName() : null);
            ref.setStatus(listing.getStatus() != null ? listing.getStatus().name() : null);
            ref.setSoldAt(listing.getSoldAt() != null ? listing.getSoldAt().toString() : null);
            ref.setBuyerName(listing.getBuyerName());
            return ref;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public String getItemDescription() { return itemDescription; }
        public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getPartNumber() { return partNumber; }
        public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
        public String getManufacturerName() { return manufacturerName; }
        public void setManufacturerName(String manufacturerName) { this.manufacturerName = manufacturerName; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getConditionName() { return conditionName; }
        public void setConditionName(String conditionName) { this.conditionName = conditionName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSoldAt() { return soldAt; }
        public void setSoldAt(String soldAt) { this.soldAt = soldAt; }
        public String getBuyerName() { return buyerName; }
        public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    }

    public static ReplayMessageDTO fromEntity(RawMessage msg, String groupName) {
        ReplayMessageDTO dto = new ReplayMessageDTO();
        dto.setId(msg.getId());
        dto.setGroupId(msg.getGroup() != null ? msg.getGroup().getId() : null);
        dto.setGroupName(groupName);
        dto.setWhapiMsgId(msg.getWhapiMsgId());
        dto.setSenderPhone(msg.getSenderPhone());
        dto.setSenderName(msg.getSenderName());
        dto.setSenderAvatar(msg.getSenderAvatar());
        dto.setMessageBody(msg.getMessageBody());
        dto.setMessageType(msg.getMessageType());
        // Prefer local media path served via /api/media; fall back to original S3 URL
        if (msg.getMediaLocalPath() != null && !msg.getMediaLocalPath().isBlank()) {
            // mediaLocalPath is like "./media/<groupId>/<filename>" — strip the "./media/" prefix
            String localPath = msg.getMediaLocalPath()
                    .replaceFirst("^\\./media/", "")
                    .replaceFirst("^media/", "");
            dto.setMediaUrl("/api/media/" + localPath);
        } else {
            dto.setMediaUrl(msg.getMediaUrl());
        }
        dto.setMediaMimeType(msg.getMediaMimeType());
        dto.setMediaLocalPath(msg.getMediaLocalPath());
        dto.setReplyToMsgId(msg.getReplyToMsgId());
        dto.setForwarded(msg.getIsForwarded() != null && msg.getIsForwarded());
        dto.setTimestampWa(msg.getTimestampWa());
        dto.setProcessed(msg.getProcessed() != null && msg.getProcessed());
        dto.setRawJson(msg.getRawJson());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getWhapiMsgId() { return whapiMsgId; }
    public void setWhapiMsgId(String whapiMsgId) { this.whapiMsgId = whapiMsgId; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getMediaMimeType() { return mediaMimeType; }
    public void setMediaMimeType(String mediaMimeType) { this.mediaMimeType = mediaMimeType; }
    public String getMediaLocalPath() { return mediaLocalPath; }
    public void setMediaLocalPath(String mediaLocalPath) { this.mediaLocalPath = mediaLocalPath; }
    public String getReplyToMsgId() { return replyToMsgId; }
    public void setReplyToMsgId(String replyToMsgId) { this.replyToMsgId = replyToMsgId; }
    @JsonProperty("isForwarded")
    public boolean isForwarded() { return isForwarded; }
    public void setForwarded(boolean forwarded) { isForwarded = forwarded; }
    public OffsetDateTime getTimestampWa() { return timestampWa; }
    public void setTimestampWa(OffsetDateTime timestampWa) { this.timestampWa = timestampWa; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
    public ExtractedListingRef getExtractedListing() { return extractedListing; }
    public void setExtractedListing(ExtractedListingRef extractedListing) { this.extractedListing = extractedListing; }
}
