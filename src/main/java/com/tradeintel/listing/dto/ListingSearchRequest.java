package com.tradeintel.listing.dto;

import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.ListingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Encapsulates all filter parameters accepted by the listing search endpoint.
 *
 * <p>All fields are optional. When a field is null it is not applied as a
 * filter predicate; only non-null values narrow the result set.</p>
 */
public class ListingSearchRequest {

    /** Filter by listing intent (sell / want / unknown). */
    private IntentType intent;

    /** Filter by category UUID. */
    private UUID categoryId;

    /** Filter by manufacturer UUID. */
    private UUID manufacturerId;

    /** Filter by condition UUID. */
    private UUID conditionId;

    /** Minimum price (inclusive). */
    private BigDecimal priceMin;

    /** Maximum price (inclusive). */
    private BigDecimal priceMax;

    /** Earliest creation date (inclusive). */
    private OffsetDateTime createdAfter;

    /** Latest creation date (inclusive). */
    private OffsetDateTime createdBefore;

    /** Filter by listing status; defaults to active in the service when null. */
    private ListingStatus status;

    /** Full-text keyword search on item description and part number. */
    private String query;

    /** Semantic query for vector similarity search. */
    private String semanticQuery;

    /** Zero-based page index. */
    private int page = 0;

    /** Page size. */
    private int size = 50;

    public ListingSearchRequest() {
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(UUID manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public UUID getConditionId() {
        return conditionId;
    }

    public void setConditionId(UUID conditionId) {
        this.conditionId = conditionId;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public OffsetDateTime getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(OffsetDateTime createdAfter) {
        this.createdAfter = createdAfter;
    }

    public OffsetDateTime getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(OffsetDateTime createdBefore) {
        this.createdBefore = createdBefore;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSemanticQuery() {
        return semanticQuery;
    }

    public void setSemanticQuery(String semanticQuery) {
        this.semanticQuery = semanticQuery;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
