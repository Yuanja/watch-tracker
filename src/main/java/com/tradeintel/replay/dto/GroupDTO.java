package com.tradeintel.replay.dto;

import com.tradeintel.common.entity.WhatsappGroup;

import java.time.OffsetDateTime;
import java.util.UUID;

public class GroupDTO {

    private UUID id;
    private String whapiGroupId;
    private String groupName;
    private String description;
    private String avatarUrl;
    private boolean isActive;
    private OffsetDateTime createdAt;

    public static GroupDTO fromEntity(WhatsappGroup group) {
        GroupDTO dto = new GroupDTO();
        dto.setId(group.getId());
        dto.setWhapiGroupId(group.getWhapiGroupId());
        dto.setGroupName(group.getGroupName());
        dto.setDescription(group.getDescription());
        dto.setAvatarUrl(group.getAvatarUrl());
        dto.setActive(group.getIsActive() != null && group.getIsActive());
        dto.setCreatedAt(group.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getWhapiGroupId() { return whapiGroupId; }
    public void setWhapiGroupId(String whapiGroupId) { this.whapiGroupId = whapiGroupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
