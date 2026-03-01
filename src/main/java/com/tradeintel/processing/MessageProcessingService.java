package com.tradeintel.processing;

import com.tradeintel.archive.EmbeddingService;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.chat.CostTrackingService;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.ReviewQueueItem;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.event.NewMessageEvent;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.normalize.JargonService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

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

    /** Matches messages that are just "Sold" with optional trailing punctuation. */
    private static final Pattern SOLD_PATTERN = Pattern.compile("(?i)^\\s*sold[!.]*\\s*$");

    private final AtomicBoolean catchupRunning = new AtomicBoolean(false);

    @Lazy
    @Autowired
    private MessageProcessingService self;

    private final RawMessageRepository rawMessageRepository;
    private final ListingRepository listingRepository;
    private final EmbeddingService embeddingService;
    private final JargonExpander jargonExpander;
    private final LLMExtractionService llmExtractionService;
    private final ConfidenceRouter confidenceRouter;
    private final ReviewQueueItemRepository reviewQueueItemRepository;
    private final NotificationMatcher notificationMatcher;
    private final JargonService jargonService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CostTrackingService costTrackingService;

    public MessageProcessingService(RawMessageRepository rawMessageRepository,
                                    ListingRepository listingRepository,
                                    EmbeddingService embeddingService,
                                    JargonExpander jargonExpander,
                                    LLMExtractionService llmExtractionService,
                                    ConfidenceRouter confidenceRouter,
                                    ReviewQueueItemRepository reviewQueueItemRepository,
                                    NotificationMatcher notificationMatcher,
                                    JargonService jargonService,
                                    SimpMessagingTemplate messagingTemplate,
                                    CostTrackingService costTrackingService) {
        this.rawMessageRepository = rawMessageRepository;
        this.listingRepository = listingRepository;
        this.embeddingService = embeddingService;
        this.jargonExpander = jargonExpander;
        this.llmExtractionService = llmExtractionService;
        this.confidenceRouter = confidenceRouter;
        this.reviewQueueItemRepository = reviewQueueItemRepository;
        this.notificationMatcher = notificationMatcher;
        this.jargonService = jargonService;
        this.messagingTemplate = messagingTemplate;
        this.costTrackingService = costTrackingService;
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

            // Short-circuit: handle "Sold" replies before full pipeline
            if (isSoldNotification(msg)) {
                handleSoldNotification(msg);
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

    /**
     * Synchronous version of the processing pipeline for on-demand extraction.
     * Runs all pipeline steps and returns the created listings.
     *
     * @param messageId the UUID of the message to process
     * @return list of created listings (may be empty)
     * @throws IllegalArgumentException if the message is not found
     */
    @Transactional
    public List<Listing> processMessageSync(UUID messageId) {
        RawMessage msg = rawMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        String originalText = msg.getMessageBody();
        if (originalText == null || originalText.isBlank()) {
            markProcessed(msg);
            return List.of();
        }

        // Short-circuit: handle "Sold" replies before full pipeline
        if (isSoldNotification(msg)) {
            return handleSoldNotification(msg);
        }

        // Generate embedding
        try {
            float[] embedding = embeddingService.embed(originalText);
            msg.setEmbedding(embedding);
            rawMessageRepository.save(msg);
        } catch (Exception e) {
            log.warn("Embedding generation failed for message {} (non-fatal): {}", messageId, e.getMessage());
        }

        // Expand jargon
        String expandedText = jargonExpander.expand(originalText);

        // LLM extraction
        ExtractionResult result = llmExtractionService.extract(expandedText, originalText);
        log.info("Sync extraction for message {}: intent={}, items={}, confidence={}",
                messageId, result.getIntent(), result.getItems().size(), result.getConfidence());

        // Track extraction cost
        trackExtractionCost(result);

        // Confidence routing
        List<Listing> listings = confidenceRouter.route(result, msg);

        // Create review queue items
        for (Listing listing : listings) {
            if (listing.getStatus() == ListingStatus.pending_review) {
                createReviewQueueItem(listing, msg, result);
            }
        }

        // Learn unknown terms
        if (result.getUnknownTerms() != null && !result.getUnknownTerms().isEmpty()) {
            try {
                jargonService.learnNewTerms(result.getUnknownTerms());
            } catch (Exception e) {
                log.warn("Jargon learning failed for message {} (non-fatal): {}", messageId, e.getMessage());
            }
        }

        // Mark processed
        markProcessed(msg);
        log.info("Sync processing completed for message {}: {} listings created", messageId, listings.size());

        return listings;
    }

    // -------------------------------------------------------------------------
    // Catchup processing
    // -------------------------------------------------------------------------

    /**
     * Processes the entire backlog of unprocessed messages asynchronously.
     * Only one catchup can run at a time — concurrent calls are rejected.
     * Messages are loaded in batches of 50, always from page 0 (since
     * processed messages drop out of the query).
     */
    @Async("processingExecutor")
    public void runCatchup() {
        if (!catchupRunning.compareAndSet(false, true)) {
            log.warn("Catchup already running; ignoring duplicate request");
            return;
        }
        try {
            log.info("Catchup started");
            int totalProcessed = 0;
            int totalErrors = 0;

            while (true) {
                Page<RawMessage> batch = rawMessageRepository.findUnprocessed(
                        PageRequest.of(0, 50, Sort.by("receivedAt").ascending()));
                if (batch.isEmpty()) {
                    break;
                }

                for (RawMessage msg : batch.getContent()) {
                    try {
                        processMessageSync(msg.getId());
                        totalProcessed++;
                    } catch (Exception e) {
                        totalErrors++;
                        log.warn("Catchup: failed to process message {}: {}",
                                msg.getId(), e.getMessage());
                    }

                    if ((totalProcessed + totalErrors) % 10 == 0) {
                        log.info("Catchup progress: processed={}, errors={}",
                                totalProcessed, totalErrors);
                    }
                }
            }

            log.info("Catchup completed: processed={}, errors={}", totalProcessed, totalErrors);
        } finally {
            catchupRunning.set(false);
        }
    }

    /**
     * Resets all processed messages and deletes re-extractable listings so the
     * catchup job can re-process them with the current extraction prompt.
     * Skips sold and soft-deleted listings (those represent real user actions).
     *
     * @return map with counts: listingsDeleted, reviewItemsDeleted, messagesReset
     */
    @Transactional
    public Map<String, Integer> resetForReprocessing() {
        // Statuses that should be re-extracted
        List<ListingStatus> reprocessStatuses = List.of(
                ListingStatus.active, ListingStatus.pending_review, ListingStatus.expired);

        // 1. Find all non-deleted listing IDs with those statuses
        List<UUID> listingIds = listingRepository.findIdsByStatusIn(reprocessStatuses);

        // 2. Delete review queue items referencing those listings (FK constraint)
        int reviewItemsDeleted = 0;
        if (!listingIds.isEmpty()) {
            reviewItemsDeleted = reviewQueueItemRepository.deleteByListingIdIn(listingIds);
        }

        // 3. Hard-delete the listings (they'll be re-created by re-extraction)
        int listingsDeleted = 0;
        if (!listingIds.isEmpty()) {
            listingsDeleted = listingRepository.deleteByStatusIn(reprocessStatuses);
        }

        // 4. Reset all processed messages so catchup re-processes them
        int messagesReset = rawMessageRepository.resetAllProcessed();

        log.info("Reset for reprocessing: {} listings deleted, {} review items deleted, {} messages reset",
                listingsDeleted, reviewItemsDeleted, messagesReset);

        return Map.of(
                "listingsDeleted", listingsDeleted,
                "reviewItemsDeleted", reviewItemsDeleted,
                "messagesReset", messagesReset);
    }

    /** Returns whether a catchup job is currently running. */
    public boolean isCatchupRunning() {
        return catchupRunning.get();
    }

    /**
     * Periodically checks for unprocessed messages and runs the catchup pipeline.
     * Default interval is 5 minutes (300000ms), configurable via
     * {@code app.processing.auto-catchup-interval-ms}.
     */
    @Scheduled(fixedRateString = "${app.processing.auto-catchup-interval-ms:300000}")
    public void scheduledCatchup() {
        if (catchupRunning.get()) {
            return;
        }
        long unprocessed = rawMessageRepository.countUnprocessed();
        if (unprocessed == 0) {
            return;
        }
        log.info("Scheduled auto-catchup triggered: {} unprocessed messages", unprocessed);
        self.runCatchup();
    }

    /** Returns the number of unprocessed messages remaining. */
    public long getUnprocessedCount() {
        return rawMessageRepository.countUnprocessed();
    }

    // -------------------------------------------------------------------------
    // Sold detection
    // -------------------------------------------------------------------------

    /**
     * Checks whether this message is a "Sold" reply — a reply to an original listing
     * whose body is just "Sold" (case-insensitive, with optional punctuation).
     */
    private boolean isSoldNotification(RawMessage msg) {
        return msg.getReplyToMsgId() != null
                && msg.getMessageBody() != null
                && SOLD_PATTERN.matcher(msg.getMessageBody()).matches();
    }

    /**
     * Handles a "Sold" reply by finding the referenced listing and marking it as sold.
     * Returns the updated listing in a list (for the sync path) or an empty list if not found.
     */
    private List<Listing> handleSoldNotification(RawMessage msg) {
        String replyToWhapiId = msg.getReplyToMsgId();
        log.info("Detected 'Sold' reply on message {}; looking up listing for referenced message {}",
                msg.getId(), replyToWhapiId);

        Optional<Listing> optListing = listingRepository.findByRawMessageWhapiMsgId(replyToWhapiId);
        if (optListing.isEmpty()) {
            log.info("No listing found for referenced message {}; marking sold reply as processed", replyToWhapiId);
            markProcessed(msg);
            return List.of();
        }

        Listing listing = optListing.get();
        if (listing.getStatus() != ListingStatus.active && listing.getStatus() != ListingStatus.pending_review) {
            log.info("Listing {} has status '{}'; not marking as sold", listing.getId(), listing.getStatus());
            markProcessed(msg);
            // Still return the listing so the UI can display it (e.g. already sold)
            return List.of(listing);
        }

        listing.setStatus(ListingStatus.sold);
        listing.setSoldAt(OffsetDateTime.now());
        listing.setSoldMessageId(msg.getWhapiMsgId());

        // Record buyer name if the sold reply sender differs from the listing seller
        if (msg.getSenderName() != null && !msg.getSenderName().equals(listing.getSenderName())) {
            listing.setBuyerName(msg.getSenderName());
        }

        listingRepository.save(listing);
        markProcessed(msg);
        log.info("Listing {} marked as sold (sold reply from {})", listing.getId(), msg.getSenderName());

        return List.of(listing);
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

    private void trackExtractionCost(ExtractionResult result) {
        if (result.getInputTokens() == 0 && result.getOutputTokens() == 0) {
            return;
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                User user = principal.getUser();
                costTrackingService.trackUsage(
                        user,
                        result.getModelUsed() != null ? result.getModelUsed() : "gpt-4o-mini",
                        result.getInputTokens(),
                        result.getOutputTokens(),
                        result.getEstimatedCostUsd()
                );
            }
        } catch (Exception e) {
            log.warn("Cost tracking failed (non-fatal): {}", e.getMessage());
        }
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
