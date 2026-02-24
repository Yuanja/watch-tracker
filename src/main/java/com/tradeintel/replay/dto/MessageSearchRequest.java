package com.tradeintel.replay.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class MessageSearchRequest {

    private UUID groupId;
    private String senderName;
    private String textQuery;
    private String semanticQuery;
    private OffsetDateTime dateFrom;
    private OffsetDateTime dateTo;

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getTextQuery() { return textQuery; }
    public void setTextQuery(String textQuery) { this.textQuery = textQuery; }
    public String getSemanticQuery() { return semanticQuery; }
    public void setSemanticQuery(String semanticQuery) { this.semanticQuery = semanticQuery; }
    public OffsetDateTime getDateFrom() { return dateFrom; }
    public void setDateFrom(OffsetDateTime dateFrom) { this.dateFrom = dateFrom; }
    public OffsetDateTime getDateTo() { return dateTo; }
    public void setDateTo(OffsetDateTime dateTo) { this.dateTo = dateTo; }
}
