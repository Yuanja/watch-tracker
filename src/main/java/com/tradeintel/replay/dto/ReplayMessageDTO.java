package com.tradeintel.replay.dto;

import com.tradeintel.common.entity.RawMessage;

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

    public ReplayMessageDTO() {}

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
        dto.setMediaUrl(msg.getMediaUrl());
        dto.setMediaMimeType(msg.getMediaMimeType());
        dto.setMediaLocalPath(msg.getMediaLocalPath());
        dto.setReplyToMsgId(msg.getReplyToMsgId());
        dto.setForwarded(msg.getIsForwarded() != null && msg.getIsForwarded());
        dto.setTimestampWa(msg.getTimestampWa());
        dto.setProcessed(msg.getProcessed() != null && msg.getProcessed());
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
    public boolean isForwarded() { return isForwarded; }
    public void setForwarded(boolean forwarded) { isForwarded = forwarded; }
    public OffsetDateTime getTimestampWa() { return timestampWa; }
    public void setTimestampWa(OffsetDateTime timestampWa) { this.timestampWa = timestampWa; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
