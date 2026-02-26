package com.tradeintel.processing;

import com.tradeintel.archive.EmbeddingService;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.ReviewQueueItem;
import com.tradeintel.common.event.NewMessageEvent;
import com.tradeintel.normalize.JargonService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the full asynchronous message processing pipeline.
 *
 * <p>Listens for {@link NewMessageEvent}s published by the webhook controller
 * after a raw message is archived. Each event is processed asynchronously on
 * the dedicated {@code processingExecutor} thread pool.</p>
 *
 * <p>Pipeline steps:
 * <ol>
 *   <li>Load the {@link RawMessage} by ID</li>
 *   <li>Generate an embedding vector via {@link EmbeddingService}</li>
 *   <li>Expand jargon acronyms via {@link JargonExpander}</li>
 *   <li>Extract structured listing data via {@link LLMExtractionService}</li>
 *   <li>Route extracted items by confidence via {@link ConfidenceRouter}</li>
 *   <li>Create {@link ReviewQueueItem} entries for pending-review listings</li>
 *   <li>Match active listings against notification rules</li>
 *   <li>Queue unknown terms for jargon auto-learning</li>
 *   <li>Mark the message as processed</li>
 * </ol>
 *
 * <p>If any step fails, the exception is caught and recorded in the message's
 * {@code processingError} field so that failed messages can be identified
 * and retried.</p>
 */
@Service
public class MessageProcessingService {

    private static final Logger log = LogManager.getLogger(MessageProcessingService.class);

    private final RawMessageRepository rawMessageRepository;
    private final EmbeddingService embeddingService;
    private final JargonExpander jargonExpander;
    private final LLMExtractionService llmExtractionService;
    private final ConfidenceRouter confidenceRouter;
    private final ReviewQueueItemRepository reviewQueueItemRepository;
    private final NotificationMatcher notificationMatcher;
    private final JargonService jargonService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageProcessingService(RawMessageRepository rawMessageRepository,
                                    EmbeddingService embeddingService,
                                    JargonExpander jargonExpander,
                                    LLMExtractionService llmExtractionService,
                                    ConfidenceRouter confidenceRouter,
                                    ReviewQueueItemRepository reviewQueueItemRepository,
                                    NotificationMatcher notificationMatcher,
                                    JargonService jargonService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.rawMessageRepository = rawMessageRepository;
        this.embeddingService = embeddingService;
        this.jargonExpander = jargonExpander;
        this.llmExtractionService = llmExtractionService;
        this.confidenceRouter = confidenceRouter;
        this.reviewQueueItemRepository = reviewQueueItemRepository;
        this.notificationMatcher = notificationMatcher;
        this.jargonService = jargonService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles a {@link NewMessageEvent} by executing the full processing pipeline.
     *
     * <p>Runs asynchronously on the {@code processingExecutor} pool and is triggered
     * after the transaction that published the event has committed
     * ({@code @TransactionalEventListener} default phase is {@code AFTER_COMMIT}).</p>
     *
     * @param event the new message event containing the message UUID
     */
    @Async("processingExecutor")
    @TransactionalEventListener
    public void onNewMessage(NewMessageEvent event) {
        UUID messageId = event.getMessageId();
        log.info("Processing pipeline started for message {}", messageId);

        RawMessage msg = null;
        try {
            // Step 1: Load the raw message
            Optional<RawMessage> optMsg = rawMessageRepository.findById(messageId);
            if (optMsg.isEmpty()) {
                log.warn("Message {} not found; skipping processing", messageId);
                return;
            }
            msg = optMsg.get();

            // Skip if already processed
            if (Boolean.TRUE.equals(msg.getProcessed())) {
                log.info("Message {} already processed; skipping", messageId);
                return;
            }

            String originalText = msg.getMessageBody();
            if (originalText == null || originalText.isBlank()) {
                log.info("Message {} has no text body; marking as processed", messageId);
                markProcessed(msg);
                return;
            }

            // Step 2: Generate embedding
            try {
                float[] embedding = embeddingService.embed(originalText);
                msg.setEmbedding(embedding);
                rawMessageRepository.save(msg);
                log.debug("Embedding generated for message {}", messageId);
            } catch (Exception e) {
                log.warn("Embedding generation failed for message {} (non-fatal): {}",
                        messageId, e.getMessage());
                // Continue processing even if embedding fails
            }

            // Step 3: Expand jargon
            String expandedText = jargonExpander.expand(originalText);
            log.debug("Jargon expansion complete for message {}", messageId);

            // Step 4: LLM extraction
            ExtractionResult result = llmExtractionService.extract(expandedText, originalText);
            log.debug("LLM extraction complete for message {}: intent={}, items={}, confidence={}",
                    messageId, result.getIntent(), result.getItems().size(), result.getConfidence());

            // Step 5: Confidence routing (creates listings)
            List<Listing> listings = confidenceRouter.route(result, msg);

            // Step 6: Create review queue items for pending-review listings
            for (Listing listing : listings) {
                if (listing.getStatus() == ListingStatus.pending_review) {
                    createReviewQueueItem(listing, msg, result);
                }
            }

            // Step 7: Match notifications for active listings and broadcast via WebSocket
            for (Listing listing : listings) {
                if (listing.getStatus() == ListingStatus.active) {
                    // Broadcast new listing event via STOMP
                    try {
                        Map<String, Object> wsPayload = new HashMap<>();
                        wsPayload.put("type", "new_listing");
                        wsPayload.put("listingId", listing.getId().toString());
                        wsPayload.put("description", listing.getItemDescription());
                        wsPayload.put("intent", listing.getIntent().name());
                        if (listing.getGroup() != null) {
                            wsPayload.put("groupId", listing.getGroup().getId().toString());
                        }
                        messagingTemplate.convertAndSend("/topic/listings", wsPayload);
                    } catch (Exception e) {
                        log.warn("WebSocket broadcast failed for listing {} (non-fatal): {}",
                                listing.getId(), e.getMessage());
                    }

                    try {
                        notificationMatcher.matchAndDispatch(listing);
                    } catch (Exception e) {
                        log.warn("Notification matching failed for listing {} (non-fatal): {}",
                                listing.getId(), e.getMessage());
                    }
                } else if (listing.getStatus() == ListingStatus.pending_review) {
                    // Broadcast review queue update via STOMP
                    try {
                        Map<String, Object> wsPayload = new HashMap<>();
                        wsPayload.put("type", "new_review_item");
                        wsPayload.put("listingId", listing.getId().toString());
                        wsPayload.put("description", listing.getItemDescription());
                        messagingTemplate.convertAndSend("/topic/review-queue", wsPayload);
                    } catch (Exception e) {
                        log.warn("WebSocket broadcast failed for review item {} (non-fatal): {}",
                                listing.getId(), e.getMessage());
                    }
                }
            }

            // Step 8: Learn new jargon terms
            if (result.getUnknownTerms() != null && !result.getUnknownTerms().isEmpty()) {
                try {
                    jargonService.learnNewTerms(result.getUnknownTerms());
                    log.debug("Queued {} unknown terms for jargon review from message {}",
                            result.getUnknownTerms().size(), messageId);
                } catch (Exception e) {
                    log.warn("Jargon learning failed for message {} (non-fatal): {}",
                            messageId, e.getMessage());
                }
            }

            // Step 9: Mark message as processed
            markProcessed(msg);
            log.info("Processing pipeline completed for message {}: {} listings created",
                    messageId, listings.size());

        } catch (Exception e) {
            log.error("Processing pipeline failed for message {}", messageId, e);
            // Step 10: Record error on the message
            if (msg != null) {
                markProcessingError(msg, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void createReviewQueueItem(Listing listing, RawMessage msg, ExtractionResult result) {
        ReviewQueueItem item = new ReviewQueueItem();
        item.setListing(listing);
        item.setRawMessage(msg);
        item.setReason("Low confidence extraction (score: " + result.getConfidence() + ")");
        item.setLlmExplanation("Extraction confidence " + result.getConfidence()
                + " is below auto-accept threshold. Intent: " + result.getIntent());
        item.setStatus("pending");

        // Store the extraction result as suggested values (JSON)
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String suggestedJson = mapper.writeValueAsString(result);
            item.setSuggestedValues(suggestedJson);
        } catch (Exception e) {
            log.warn("Failed to serialize suggested values for review item: {}", e.getMessage());
            item.setSuggestedValues("{}");
        }

        reviewQueueItemRepository.save(item);
        log.debug("Created review queue item for listing {} from message {}",
                listing.getId(), msg.getId());
    }

    private void markProcessed(RawMessage msg) {
        msg.setProcessed(true);
        msg.setProcessingError(null);
        rawMessageRepository.save(msg);
    }

    private void markProcessingError(RawMessage msg, Exception e) {
        try {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 2000);
            }
            msg.setProcessingError(errorMsg);
            rawMessageRepository.save(msg);
        } catch (Exception saveErr) {
            log.error("Failed to record processing error on message {}", msg.getId(), saveErr);
        }
    }
}
