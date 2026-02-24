package com.tradeintel.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregated daily cost-tracking record for a single user's OpenAI API usage.
 *
 * <p>One row exists per (user, date) pair, enforced by a unique constraint.
 * The pipeline increments the counters atomically for each LLM call so that
 * per-user cost reports can be produced without scanning raw log data.</p>
 *
 * Maps to the {@code usage_ledger} table.
 */
@Entity
@Table(
    name = "usage_ledger",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_usage_ledger_user_date",
        columnNames = {"user_id", "period_date"}
    )
)
public class UsageLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The user whose API usage this record aggregates. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The calendar day this record covers. */
    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Column(name = "total_input_tokens")
    private Long totalInputTokens = 0L;

    @Column(name = "total_output_tokens")
    private Long totalOutputTokens = 0L;

    /** Cumulative USD cost for all LLM calls on this day. */
    @Column(name = "total_cost_usd", precision = 12, scale = 6)
    private BigDecimal totalCostUsd = BigDecimal.ZERO;

    /** Number of chat sessions started on this day. */
    @Column(name = "session_count")
    private Integer sessionCount = 0;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public UsageLedger() {
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

    public LocalDate getPeriodDate() {
        return periodDate;
    }

    public void setPeriodDate(LocalDate periodDate) {
        this.periodDate = periodDate;
    }

    public Long getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(Long totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public Long getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(Long totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public BigDecimal getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(BigDecimal totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public Integer getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(Integer sessionCount) {
        this.sessionCount = sessionCount;
    }
}
