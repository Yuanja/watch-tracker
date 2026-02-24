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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Full archive of every WhatsApp message received via Whapi.cloud webhooks.
 * This is the source of truth for the WhatsApp replay UI and the input to the
 * async processing pipeline.
 *
 * <p>The {@code embedding} field maps to a PostgreSQL {@code vector(1536)} column
 * produced by the OpenAI {@code text-embedding-3-small} model. It is stored as a
 * {@code float[]} and mapped with {@code @JdbcTypeCode(SqlTypes.VECTOR)} so that
 * Hibernate's pgvector dialect support can handle it against a real PostgreSQL
 * data source. Against H2 the column is not present so this field is effectively
 * ignored during H2-based tests.</p>
 *
 * Maps to the {@code raw_messages} table.
 */
@Entity
@Table(name = "raw_messages")
public class RawMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The WhatsApp group this message was received in. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private WhatsappGroup group;

    /** Unique message identifier assigned by Whapi.cloud. Enforces idempotency. */
    @Column(name = "whapi_msg_id", unique = true, nullable = false)
    private String whapiMsgId;

    @Column(name = "sender_phone")
    private String senderPhone;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_avatar")
    private String senderAvatar;

    @Column(name = "message_body", columnDefinition = "text")
    private String messageBody;

    /** Type of WhatsApp message: text, image, document, video, audio. */
    @Column(name = "message_type")
    private String messageType = "text";

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "media_mime_type")
    private String mediaMimeType;

    /** Path to the locally cached or S3-stored copy of the media file. */
    @Column(name = "media_local_path")
    private String mediaLocalPath;

    /** Whapi message ID of the message this one quotes/replies to, if any. */
    @Column(name = "reply_to_msg_id")
    private String replyToMsgId;

    @Column(name = "is_forwarded")
    private Boolean isForwarded = false;

    /** When the message was sent in WhatsApp (from the Whapi payload). */
    @Column(name = "timestamp_wa", nullable = false)
    private OffsetDateTime timestampWa;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private OffsetDateTime receivedAt;

    // Processing state ---------------------------------------------------------

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;

    // Semantic embedding -------------------------------------------------------

    /**
     * 1536-dimensional embedding vector produced by {@code text-embedding-3-small}.
     * Maps to {@code vector(1536)} in PostgreSQL via Hibernate's pgvector support.
     * The {@code SqlTypes.VECTOR} JDBC type code is recognised by the
     * {@code pgvector-hibernate} integration included on the runtime classpath.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private float[] embedding;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public RawMessage() {
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

    public WhatsappGroup getGroup() {
        return group;
    }

    public void setGroup(WhatsappGroup group) {
        this.group = group;
    }

    /**
     * Convenience accessor that returns the group's UUID without requiring
     * callers to navigate the lazy-loaded {@link WhatsappGroup} relationship.
     * Returns {@code null} if no group has been assigned.
     */
    public UUID getGroupId() {
        return group != null ? group.getId() : null;
    }

    public String getWhapiMsgId() {
        return whapiMsgId;
    }

    public void setWhapiMsgId(String whapiMsgId) {
        this.whapiMsgId = whapiMsgId;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaMimeType() {
        return mediaMimeType;
    }

    public void setMediaMimeType(String mediaMimeType) {
        this.mediaMimeType = mediaMimeType;
    }

    public String getMediaLocalPath() {
        return mediaLocalPath;
    }

    public void setMediaLocalPath(String mediaLocalPath) {
        this.mediaLocalPath = mediaLocalPath;
    }

    public String getReplyToMsgId() {
        return replyToMsgId;
    }

    public void setReplyToMsgId(String replyToMsgId) {
        this.replyToMsgId = replyToMsgId;
    }

    public Boolean getIsForwarded() {
        return isForwarded;
    }

    public void setIsForwarded(Boolean isForwarded) {
        this.isForwarded = isForwarded;
    }

    public OffsetDateTime getTimestampWa() {
        return timestampWa;
    }

    public void setTimestampWa(OffsetDateTime timestampWa) {
        this.timestampWa = timestampWa;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
