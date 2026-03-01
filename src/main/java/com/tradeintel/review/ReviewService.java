package com.tradeintel.review;

import com.tradeintel.admin.AuditService;
import com.tradeintel.common.entity.Category;
import com.tradeintel.common.entity.Condition;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.Manufacturer;
import com.tradeintel.common.entity.ReviewQueueItem;
import com.tradeintel.common.entity.Unit;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.normalize.ConditionRepository;
import com.tradeintel.normalize.ManufacturerRepository;
import com.tradeintel.normalize.UnitRepository;
import com.tradeintel.processing.ExtractionResult;
import com.tradeintel.processing.LLMExtractionService;
import com.tradeintel.processing.ReviewQueueItemRepository;
import com.tradeintel.review.dto.AssistResponse;
import com.tradeintel.review.dto.ResolutionRequest;
import com.tradeintel.review.dto.ReviewItemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for the admin review queue.
 *
 * <p>Provides operations to list pending review items, resolve items with
 * corrections, and skip items. Resolving an item updates the associated
 * listing with the admin's corrections and promotes it to active status.</p>
 */
@Service
@Transactional
public class ReviewService {

    private static final Logger log = LogManager.getLogger(ReviewService.class);

    private final ReviewQueueItemRepository reviewQueueItemRepository;
    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final UnitRepository unitRepository;
    private final ConditionRepository conditionRepository;
    private final AuditService auditService;
    private final LLMExtractionService llmExtractionService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public ReviewService(ReviewQueueItemRepository reviewQueueItemRepository,
                         ListingRepository listingRepository,
                         CategoryRepository categoryRepository,
                         ManufacturerRepository manufacturerRepository,
                         UnitRepository unitRepository,
                         ConditionRepository conditionRepository,
                         AuditService auditService,
                         LLMExtractionService llmExtractionService) {
        this.reviewQueueItemRepository = reviewQueueItemRepository;
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.unitRepository = unitRepository;
        this.conditionRepository = conditionRepository;
        this.auditService = auditService;
        this.llmExtractionService = llmExtractionService;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // List pending
    // -------------------------------------------------------------------------

    /**
     * Returns a page of pending review items, ordered by creation time ascending
     * (oldest first for FIFO processing).
     *
     * @param page zero-based page index
     * @param size page size
     * @return page of {@link ReviewItemDTO}
     */
    @Transactional(readOnly = true)
    public Page<ReviewItemDTO> listPending(int page, int size) {
        Page<ReviewQueueItem> items = reviewQueueItemRepository
                .findByStatusOrderByCreatedAtAsc("pending", PageRequest.of(page, size));
        log.debug("Listed pending reviews: page={}, size={}, total={}",
                page, size, items.getTotalElements());
        return items.map(ReviewItemDTO::fromEntity);
    }

    // -------------------------------------------------------------------------
    // Resolve
    // -------------------------------------------------------------------------

    /**
     * Resolves a review item by applying admin corrections to the associated listing
     * and promoting it to active status.
     *
     * @param id       the UUID of the review queue item
     * @param request  the admin's corrections
     * @param resolver the admin user performing the resolution
     * @return the updated review item as a DTO
     * @throws ResourceNotFoundException if the review item or its listing is not found
     */
    public ReviewItemDTO resolve(UUID id, ResolutionRequest request, User resolver) {
        ReviewQueueItem item = reviewQueueItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewQueueItem", id));

        if (!"pending".equals(item.getStatus())) {
            throw new IllegalArgumentException(
                    "Review item " + id + " is already " + item.getStatus());
        }

        Listing listing = item.getListing();
        if (listing == null) {
            throw new ResourceNotFoundException(
                    "No listing associated with review item " + id);
        }

        // Apply corrections from the resolution request
        applyCorrections(listing, request);

        // Promote to active
        listing.setStatus(ListingStatus.active);
        listing.setNeedsHumanReview(false);
        listing.setReviewedBy(resolver);
        listing.setReviewedAt(OffsetDateTime.now());
        listingRepository.save(listing);

        // Mark the review item as resolved
        item.setStatus("resolved");
        item.setResolvedBy(resolver);
        item.setResolvedAt(OffsetDateTime.now());

        // Store the resolution as JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            item.setResolution(mapper.writeValueAsString(request));
        } catch (Exception e) {
            log.warn("Failed to serialize resolution for review item {}: {}", id, e.getMessage());
            item.setResolution("{}");
        }

        ReviewQueueItem saved = reviewQueueItemRepository.save(item);
        log.info("Resolved review item {} for listing {} by user {}",
                id, listing.getId(), resolver.getId());
        auditService.log(resolver.getId(), "review.resolve", "ReviewQueueItem", id, null, null, null);
        return ReviewItemDTO.fromEntity(saved);
    }

    // -------------------------------------------------------------------------
    // Skip
    // -------------------------------------------------------------------------

    /**
     * Marks a review item as skipped without modifying the associated listing.
     *
     * @param id       the UUID of the review queue item
     * @param resolver the admin user who skipped the item
     * @return the updated review item as a DTO
     * @throws ResourceNotFoundException if the review item is not found
     */
    public ReviewItemDTO skip(UUID id, User resolver) {
        ReviewQueueItem item = reviewQueueItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewQueueItem", id));

        if (!"pending".equals(item.getStatus())) {
            throw new IllegalArgumentException(
                    "Review item " + id + " is already " + item.getStatus());
        }

        item.setStatus("skipped");
        item.setResolvedBy(resolver);
        item.setResolvedAt(OffsetDateTime.now());

        ReviewQueueItem saved = reviewQueueItemRepository.save(item);
        log.info("Skipped review item {} by user {}", id, resolver.getId());
        auditService.log(resolver.getId(), "review.skip", "ReviewQueueItem", id, null, null, null);
        return ReviewItemDTO.fromEntity(saved);
    }

    // -------------------------------------------------------------------------
    // Agent-assisted review
    // -------------------------------------------------------------------------

    /**
     * Uses the LLM to refine extraction for a listing based on an admin's hint.
     * Looks up the pending review queue item for the given listing, sends the
     * original text plus the hint to the LLM, and persists the updated
     * suggested values for iterative refinement.
     *
     * @param listingId the listing UUID
     * @param hint      the reviewer's natural-language guidance
     * @return the refined extraction result with original text
     * @throws ResourceNotFoundException if no pending review item exists for the listing
     */
    public AssistResponse assistByListing(UUID listingId, String hint) {
        List<ReviewQueueItem> items = reviewQueueItemRepository.findByListingId(listingId);
        ReviewQueueItem item = items.stream()
                .filter(i -> "pending".equals(i.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending review item for listing " + listingId));

        String originalText = item.getRawMessage() != null
                ? item.getRawMessage().getMessageBody() : "";
        String previousExtraction = item.getSuggestedValues() != null
                ? item.getSuggestedValues() : "{}";

        ExtractionResult result = llmExtractionService.extractWithHint(
                originalText, previousExtraction, hint);

        // Persist updated suggested values for iterative refinement
        try {
            String updatedJson = objectMapper.writeValueAsString(result);
            item.setSuggestedValues(updatedJson);
            reviewQueueItemRepository.save(item);
        } catch (Exception e) {
            log.warn("Failed to persist updated suggested values for listing {}: {}",
                    listingId, e.getMessage());
        }

        log.info("Agent-assisted extraction for listing {}: confidence={}",
                listingId, result.getConfidence());
        return new AssistResponse(result, originalText);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies non-null corrections from the resolution request to the listing.
     */
    private void applyCorrections(Listing listing, ResolutionRequest request) {
        if (request.getItemDescription() != null && !request.getItemDescription().isBlank()) {
            listing.setItemDescription(request.getItemDescription().trim());
        }

        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            Category category = categoryRepository
                    .findByNameIgnoreCase(request.getCategoryName().trim())
                    .orElse(null);
            listing.setItemCategory(category);
        }

        if (request.getManufacturerName() != null && !request.getManufacturerName().isBlank()) {
            Manufacturer manufacturer = manufacturerRepository
                    .findByNameIgnoreCase(request.getManufacturerName().trim())
                    .orElse(null);
            listing.setManufacturer(manufacturer);
        }

        if (request.getPartNumber() != null) {
            listing.setPartNumber(request.getPartNumber().trim());
        }

        if (request.getQuantity() != null) {
            listing.setQuantity(BigDecimal.valueOf(request.getQuantity()));
        }

        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            Unit unit = unitRepository.findByNameIgnoreCase(request.getUnit().trim())
                    .or(() -> unitRepository.findByAbbreviationIgnoreCase(request.getUnit().trim()))
                    .orElse(null);
            listing.setUnit(unit);
        }

        if (request.getPrice() != null) {
            listing.setPrice(BigDecimal.valueOf(request.getPrice()));
        }

        if (request.getCondition() != null && !request.getCondition().isBlank()) {
            Condition condition = conditionRepository
                    .findByNameIgnoreCase(request.getCondition().trim())
                    .orElse(null);
            listing.setCondition(condition);
        }

        if (request.getIntent() != null && !request.getIntent().isBlank()) {
            try {
                listing.setIntent(IntentType.valueOf(request.getIntent().toLowerCase().trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid intent '{}' in resolution request; keeping original",
                        request.getIntent());
            }
        }
    }
}
