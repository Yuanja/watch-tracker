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
 * A user-defined notification rule expressed in natural language.
 *
 * <p>When the user submits a rule such as "notify me when Parker valves appear",
 * the NL rule parser (via OpenAI) extracts structured filter criteria
 * ({@code parsedIntent}, {@code parsedKeywords}, {@code parsedCategoryIds},
 * price bounds, etc.) which are then evaluated against each new listing.</p>
 *
 * <p>The {@code parsedKeywords} and {@code parsedCategoryIds} fields map to
 * PostgreSQL {@code TEXT[]} and {@code UUID[]} array columns respectively,
 * annotated with {@code @JdbcTypeCode(SqlTypes.ARRAY)} for Hibernate's
 * PostgreSQL array support.</p>
 *
 * Maps to the {@code notification_rules} table.
 */
@Entity
@Table(name = "notification_rules")
public class NotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The user who owns this rule. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The original natural-language rule as typed by the user. */
    @Column(name = "nl_rule", nullable = false, columnDefinition = "text")
    private String nlRule;

    /**
     * The LLM-parsed intent filter extracted from {@code nlRule}.
     * Stored as VARCHAR; Flyway creates the {@code intent_type} PG enum for production.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "parsed_intent")
    private IntentType parsedIntent;

    /**
     * Keywords extracted from the natural-language rule for fuzzy text matching.
     * Stored as {@code TEXT[]} in PostgreSQL.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "parsed_keywords", columnDefinition = "text[]")
    private String[] parsedKeywords;

    /**
     * Category UUIDs extracted from the natural-language rule.
     * Stored as {@code UUID[]} in PostgreSQL.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "parsed_category_ids", columnDefinition = "uuid[]")
    private UUID[] parsedCategoryIds;

    @Column(name = "parsed_price_min", precision = 19, scale = 4)
    private BigDecimal parsedPriceMin;

    @Column(name = "parsed_price_max", precision = 19, scale = 4)
    private BigDecimal parsedPriceMax;

    /**
     * Delivery channel for matched notifications.
     * Currently only {@code "email"} is supported.
     */
    @Column(name = "notify_channel")
    private String notifyChannel = "email";

    /** Email address to send notifications to (defaults to the user's account email). */
    @Column(name = "notify_email")
    private String notifyEmail;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_triggered")
    private OffsetDateTime lastTriggered;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public NotificationRule() {
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNlRule() {
        return nlRule;
    }

    public void setNlRule(String nlRule) {
        this.nlRule = nlRule;
    }

    public IntentType getParsedIntent() {
        return parsedIntent;
    }

    public void setParsedIntent(IntentType parsedIntent) {
        this.parsedIntent = parsedIntent;
    }

    public String[] getParsedKeywords() {
        return parsedKeywords;
    }

    public void setParsedKeywords(String[] parsedKeywords) {
        this.parsedKeywords = parsedKeywords;
    }

    public UUID[] getParsedCategoryIds() {
        return parsedCategoryIds;
    }

    public void setParsedCategoryIds(UUID[] parsedCategoryIds) {
        this.parsedCategoryIds = parsedCategoryIds;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
