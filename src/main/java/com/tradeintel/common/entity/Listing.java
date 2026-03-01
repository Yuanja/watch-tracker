package com.tradeintel.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A structured trade listing extracted by the LLM pipeline from a raw WhatsApp message.
 *
 * <p>Both {@code intent} and {@code status} are stored as VARCHAR strings via
 * {@code @Enumerated(EnumType.STRING)} so that H2-based integration tests work without
 * a PostgreSQL custom enum; the Flyway migration defines the actual PG enums
 * ({@code intent_type}, {@code listing_status}) for production.</p>
 *
 * <p>The {@code embedding} field maps to a PostgreSQL {@code vector(1536)} column via
 * {@code @JdbcTypeCode(SqlTypes.VECTOR)}, handled by the pgvector-hibernate integration.</p>
 *
 * Maps to the {@code listings} table.
 */
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Provenance ---------------------------------------------------------------

    /** The raw WhatsApp message this listing was extracted from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_message_id", nullable = false)
    private RawMessage rawMessage;

    /** The WhatsApp group in which the originating message was posted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private WhatsappGroup group;

    // Classification -----------------------------------------------------------

    /**
     * Whether the poster is selling or looking to buy.
     * Stored as VARCHAR; Flyway creates the {@code intent_type} PG enum for production.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false)
    private IntentType intent;

    /** LLM confidence in the extraction, 0.0 – 1.0. */
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore = 0.0;

    // Normalised fields --------------------------------------------------------

    @Column(name = "item_description", nullable = false, columnDefinition = "text")
    private String itemDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_category_id")
    private Category itemCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    @Column(name = "part_number")
    private String partNumber;

    @Column(name = "quantity", precision = 19, scale = 4)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "price_currency")
    private String priceCurrency = "USD";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    private Condition condition;

    // Original content ---------------------------------------------------------

    @Column(name = "original_text", nullable = false, columnDefinition = "text")
    private String originalText;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_phone")
    private String senderPhone;

    // Review state -------------------------------------------------------------

    /**
     * Lifecycle status of this listing.
     * Stored as VARCHAR; Flyway creates the {@code listing_status} PG enum for production.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ListingStatus status = ListingStatus.active;

    @Column(name = "needs_human_review")
    private Boolean needsHumanReview = false;

    /** The admin user who reviewed this listing, if applicable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    // Semantic embedding -------------------------------------------------------

    /**
     * 1536-dimensional embedding of the listing description.
     * Maps to {@code vector(1536)} in PostgreSQL via Hibernate's pgvector support.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    // Lifecycle ----------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    // Sold tracking ----------------------------------------------------------

    /** Timestamp when the listing was marked as sold. */
    @Column(name = "sold_at")
    private OffsetDateTime soldAt;

    /** Whapi message ID of the "sold" reply, for traceability. */
    @Column(name = "sold_message_id")
    private String soldMessageId;

    /** Name of the buyer (sender of the "sold" reply, if different from listing seller). */
    @Column(name = "buyer_name")
    private String buyerName;

    // Soft delete --------------------------------------------------------------

    /** Soft-delete timestamp; null means the listing is not deleted. */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /** The user who performed the soft delete. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Listing() {
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

    public RawMessage getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(RawMessage rawMessage) {
        this.rawMessage = rawMessage;
    }

    public WhatsappGroup getGroup() {
        return group;
    }

    public void setGroup(WhatsappGroup group) {
        this.group = group;
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

    public Category getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(Category itemCategory) {
        this.itemCategory = itemCategory;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
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

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
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

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
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

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
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

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
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
