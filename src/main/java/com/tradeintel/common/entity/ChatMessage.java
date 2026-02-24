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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single message within a {@link ChatSession}, representing either a user query,
 * an AI assistant response, or a system prompt.
 *
 * <p>The {@code toolCalls} field stores the raw JSON array of OpenAI function-calling
 * tool invocations (if any) as a {@code JSONB} column. It is surfaced as a
 * {@code String} here; callers are responsible for serialising/deserialising with Jackson.
 * The {@code columnDefinition = "jsonb"} hints to Hibernate to use the JSONB wire type
 * when communicating with PostgreSQL.</p>
 *
 * Maps to the {@code chat_messages} table.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The session this message belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    /**
     * OpenAI chat role: {@code user}, {@code assistant}, or {@code system}.
     * Enforced by a CHECK constraint in the Flyway migration.
     */
    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    /** The OpenAI model identifier used to generate this message, e.g. "gpt-4o". */
    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "input_tokens")
    private Integer inputTokens = 0;

    @Column(name = "output_tokens")
    private Integer outputTokens = 0;

    /** Monetary cost in USD for this message's token usage. */
    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd = BigDecimal.ZERO;

    /**
     * JSON array of OpenAI function-calling tool invocations, stored as JSONB.
     * {@code null} when no tools were called. Callers should use Jackson to
     * parse this into a typed structure before use.
     */
    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private String toolCalls;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ChatMessage() {
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

    public ChatSession getSession() {
        return session;
    }

    public void setSession(ChatSession session) {
        this.session = session;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public String getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(String toolCalls) {
        this.toolCalls = toolCalls;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
