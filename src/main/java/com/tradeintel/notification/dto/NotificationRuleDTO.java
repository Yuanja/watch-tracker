package com.tradeintel.notification.dto;

import com.tradeintel.common.entity.NotificationRule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Read-only response DTO for a {@link NotificationRule} entity.
 *
 * <p>Converts array fields to lists and exposes all relevant fields
 * for the notification management UI.</p>
 */
public class NotificationRuleDTO {

    private UUID id;
    private String nlRule;
    private String parsedIntent;
    private List<String> parsedKeywords;
    private BigDecimal parsedPriceMin;
    private BigDecimal parsedPriceMax;
    private String notifyChannel;
    private String notifyEmail;
    private Boolean isActive;
    private OffsetDateTime lastTriggered;
    private OffsetDateTime createdAt;

    public NotificationRuleDTO() {
    }

    /**
     * Maps a {@link NotificationRule} entity to its DTO representation.
     *
     * @param rule the entity to map; must not be null
     * @return the populated DTO
     */
    public static NotificationRuleDTO fromEntity(NotificationRule rule) {
        NotificationRuleDTO dto = new NotificationRuleDTO();
        dto.id = rule.getId();
        dto.nlRule = rule.getNlRule();
        dto.parsedIntent = rule.getParsedIntent() != null ? rule.getParsedIntent().name() : null;
        dto.parsedKeywords = rule.getParsedKeywords() != null
                ? Arrays.asList(rule.getParsedKeywords())
                : List.of();
        dto.parsedPriceMin = rule.getParsedPriceMin();
        dto.parsedPriceMax = rule.getParsedPriceMax();
        dto.notifyChannel = rule.getNotifyChannel();
        dto.notifyEmail = rule.getNotifyEmail();
        dto.isActive = rule.getIsActive();
        dto.lastTriggered = rule.getLastTriggered();
        dto.createdAt = rule.getCreatedAt();
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

    public String getNlRule() {
        return nlRule;
    }

    public void setNlRule(String nlRule) {
        this.nlRule = nlRule;
    }

    public String getParsedIntent() {
        return parsedIntent;
    }

    public void setParsedIntent(String parsedIntent) {
        this.parsedIntent = parsedIntent;
    }

    public List<String> getParsedKeywords() {
        return parsedKeywords;
    }

    public void setParsedKeywords(List<String> parsedKeywords) {
        this.parsedKeywords = parsedKeywords;
    }

    public BigDecimal getParsedPriceMin() {
        return parsedPriceMin;
    }

    public void setParsedPriceMin(BigDecimal parsedPriceMin) {
        this.parsedPriceMin = parsedPriceMin;
    }

    public BigDecimal getParsedPriceMax() {
        return parsedPriceMax;
    }

    public void setParsedPriceMax(BigDecimal parsedPriceMax) {
        this.parsedPriceMax = parsedPriceMax;
    }

    public String getNotifyChannel() {
        return notifyChannel;
    }

    public void setNotifyChannel(String notifyChannel) {
        this.notifyChannel = notifyChannel;
    }

    public String getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(String notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getLastTriggered() {
        return lastTriggered;
    }

    public void setLastTriggered(OffsetDateTime lastTriggered) {
        this.lastTriggered = lastTriggered;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
