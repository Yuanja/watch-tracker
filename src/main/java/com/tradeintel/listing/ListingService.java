package com.tradeintel.listing;

import com.tradeintel.admin.AuditService;
import com.tradeintel.common.entity.Category;
import com.tradeintel.common.entity.Condition;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.Manufacturer;
import com.tradeintel.common.entity.Unit;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.listing.dto.ListingDTO;
import com.tradeintel.listing.dto.ListingSearchRequest;
import com.tradeintel.listing.dto.ListingStatsDTO;
import com.tradeintel.listing.dto.ListingUpdateRequest;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.normalize.ConditionRepository;
import com.tradeintel.normalize.ManufacturerRepository;
import com.tradeintel.normalize.UnitRepository;
import com.tradeintel.processing.ExchangeRateService;
import com.tradeintel.processing.ExtractionResult;
import com.tradeintel.processing.LLMExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradeintel.listing.dto.CrossPostDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for listing CRUD, search, and soft-delete operations.
 *
 * <p>Search uses JPA Criteria (via {@link ListingSpecification}) for Phase 2.
 * Semantic pgvector search will be layered on in Phase 3 without changing this
 * service's public interface.</p>
 */
@Service
@Transactional
public class ListingService {

    private static final Logger log = LogManager.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final UnitRepository unitRepository;
    private final ConditionRepository conditionRepository;
    private final EntityManager entityManager;
    private final AuditService auditService;
    private final LLMExtractionService llmExtractionService;
    private final ExchangeRateService exchangeRateService;
    private final ObjectMapper objectMapper;

    public ListingService(ListingRepository listingRepository,
                          CategoryRepository categoryRepository,
                          ManufacturerRepository manufacturerRepository,
                          UnitRepository unitRepository,
                          ConditionRepository conditionRepository,
                          EntityManager entityManager,
                          AuditService auditService,
                          LLMExtractionService llmExtractionService,
                          ExchangeRateService exchangeRateService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.unitRepository = unitRepository;
        this.conditionRepository = conditionRepository;
        this.entityManager = entityManager;
        this.auditService = auditService;
        this.llmExtractionService = llmExtractionService;
        this.exchangeRateService = exchangeRateService;
        this.objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Search / list
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated, filtered page of non-deleted listings.
     *
     * <p>Defaults to {@code status = active} when the caller does not specify
     * a status. Results are sorted by {@code createdAt} descending.</p>
     *
     * @param request filter and pagination parameters
     * @return page of {@link ListingDTO}
     */
    @Transactional(readOnly = true)
    public Page<ListingDTO> list(ListingSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), 200);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Listing> spec = ListingSpecification.from(request);
        Page<Listing> listings = listingRepository.findAll(spec, pageable);

        log.debug("Listing search: total={}, page={}, size={}", listings.getTotalElements(), page, size);
        return listings.map(ListingDTO::fromEntity);
    }

    // -------------------------------------------------------------------------
    // Get by id
    // -------------------------------------------------------------------------

    /**
     * Returns a single listing by id.
     * Soft-deleted listings are still retrievable by id (for audit / admin use).
     *
     * @param id the listing UUID
     * @return the listing as a DTO
     * @throws ResourceNotFoundException if no listing with the given id exists
     */
    @Transactional(readOnly = true)
    public ListingDTO getById(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));
        return ListingDTO.fromEntity(listing);
    }

    // -------------------------------------------------------------------------
    // Update (admin)
    // -------------------------------------------------------------------------

    /**
     * Applies a partial update to a listing.
     * Only non-null fields in the request are modified; related entities
     * (category, manufacturer, unit, condition) are resolved via
     * {@link EntityManager#getReference} to avoid unnecessary selects.
     *
     * @param id      the listing UUID
     * @param request partial update payload
     * @return the updated listing as a DTO
     * @throws ResourceNotFoundException if the listing or any referenced entity is not found
     */
    public ListingDTO update(UUID id, ListingUpdateRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        if (request.getIntent() != null) {
            listing.setIntent(request.getIntent());
        }
        if (request.getItemDescription() != null && !request.getItemDescription().isBlank()) {
            listing.setItemDescription(request.getItemDescription().trim());
        }
        if (request.getItemCategoryId() != null) {
            listing.setItemCategory(entityManager.getReference(Category.class, request.getItemCategoryId()));
        }
        if (request.getManufacturerId() != null) {
            listing.setManufacturer(entityManager.getReference(Manufacturer.class, request.getManufacturerId()));
        }
        if (request.getPartNumber() != null) {
            listing.setPartNumber(request.getPartNumber().trim());
        }
        if (request.getQuantity() != null) {
            listing.setQuantity(request.getQuantity());
        }
        if (request.getUnitId() != null) {
            listing.setUnit(entityManager.getReference(Unit.class, request.getUnitId()));
        }
        if (request.getPrice() != null) {
            listing.setPrice(request.getPrice());
        }
        if (request.getPriceCurrency() != null && !request.getPriceCurrency().isBlank()) {
            listing.setPriceCurrency(request.getPriceCurrency().trim().toUpperCase());
        }
        if (request.getConditionId() != null) {
            listing.setCondition(entityManager.getReference(Condition.class, request.getConditionId()));
        }
        if (request.getStatus() != null) {
            listing.setStatus(request.getStatus());
        }
        if (request.getNeedsHumanReview() != null) {
            listing.setNeedsHumanReview(request.getNeedsHumanReview());
        }
        if (request.getExpiresAt() != null) {
            listing.setExpiresAt(request.getExpiresAt());
        }

        // Recalculate exchange rate if price or currency changed
        if (request.getPrice() != null || request.getPriceCurrency() != null) {
            recalculateExchangeRate(listing);
        }

        Listing saved = listingRepository.save(listing);
        log.info("Updated listing: id={}", saved.getId());
        auditService.log(null, "listing.update", "Listing", id, null, null, null);
        return ListingDTO.fromEntity(saved);
    }

    // -------------------------------------------------------------------------
    // Soft delete (uber admin)
    // -------------------------------------------------------------------------

    /**
     * Soft-deletes a listing by setting {@code deletedAt} and {@code deletedBy}.
     * The record remains in the database for audit purposes and is excluded from
     * normal search results via the {@code deletedAt IS NULL} predicate.
     *
     * @param id          the listing UUID
     * @param deletedByUser the User performing the deletion (resolved from JWT principal)
     * @throws ResourceNotFoundException if no listing with the given id exists
     */
    public void softDelete(UUID id, User deletedByUser) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        listing.setDeletedAt(OffsetDateTime.now());
        listing.setDeletedBy(deletedByUser);
        listing.setStatus(ListingStatus.deleted);

        listingRepository.save(listing);
        log.info("Soft-deleted listing: id={}, deletedBy={}", id, deletedByUser.getId());
        auditService.log(deletedByUser.getId(), "listing.soft_delete", "Listing", id, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    /**
     * Returns aggregated counts by intent and by status.
     *
     * @return {@link ListingStatsDTO} with totals and breakdowns
     */
    @Transactional(readOnly = true)
    public ListingStatsDTO getStats() {
        long total = listingRepository.countByStatusAndNotDeleted(ListingStatus.active)
                + listingRepository.countByStatusAndNotDeleted(ListingStatus.sold)
                + listingRepository.countByStatusAndNotDeleted(ListingStatus.pending_review)
                + listingRepository.countByStatusAndNotDeleted(ListingStatus.expired);

        Map<String, Long> byIntent = Map.of(
                IntentType.sell.name(), listingRepository.countByIntent(IntentType.sell),
                IntentType.want.name(), listingRepository.countByIntent(IntentType.want),
                IntentType.unknown.name(), listingRepository.countByIntent(IntentType.unknown)
        );

        Map<String, Long> byStatus = Map.of(
                ListingStatus.active.name(), listingRepository.countByStatus(ListingStatus.active),
                ListingStatus.sold.name(), listingRepository.countByStatus(ListingStatus.sold),
                ListingStatus.expired.name(), listingRepository.countByStatus(ListingStatus.expired),
                ListingStatus.deleted.name(), listingRepository.countByStatus(ListingStatus.deleted),
                ListingStatus.pending_review.name(), listingRepository.countByStatus(ListingStatus.pending_review)
        );

        log.debug("Stats: total={}", total);
        return new ListingStatsDTO(total, byIntent, byStatus);
    }

    // -------------------------------------------------------------------------
    // Retry extraction
    // -------------------------------------------------------------------------

    /**
     * Re-runs LLM extraction on a listing's original text with an optional
     * reviewer hint, then applies the result directly to the listing.
     *
     * @param id   the listing UUID
     * @param hint optional natural-language guidance (may be null or blank)
     * @return the updated listing as a DTO
     */
    public ListingDTO retryExtraction(UUID id, String hint) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id));

        String originalText = listing.getOriginalText();
        if (originalText == null || originalText.isBlank()) {
            throw new IllegalArgumentException("Listing has no original text to re-extract");
        }

        // Build a JSON snapshot of the current extraction for context
        String previousExtraction = buildCurrentExtractionJson(listing);

        ExtractionResult result;
        if (hint != null && !hint.isBlank()) {
            result = llmExtractionService.extractWithHint(originalText, previousExtraction, hint);
        } else {
            result = llmExtractionService.extract(originalText, originalText);
        }

        // Apply the first extracted item to the listing
        applyExtraction(listing, result);

        // Recalculate exchange rate after re-extraction
        recalculateExchangeRate(listing);

        Listing saved = listingRepository.save(listing);
        log.info("Retry extraction for listing {}: confidence={}", id, result.getConfidence());
        auditService.log(null, "listing.retry_extraction", "Listing", id, null, null, null);
        return ListingDTO.fromEntity(saved);
    }

    private String buildCurrentExtractionJson(Listing listing) {
        try {
            Map<String, Object> current = Map.of(
                    "intent", listing.getIntent() != null ? listing.getIntent().name() : "unknown",
                    "confidence", listing.getConfidenceScore() != null ? listing.getConfidenceScore() : 0.0,
                    "items", java.util.List.of(Map.of(
                            "description", listing.getItemDescription() != null ? listing.getItemDescription() : "",
                            "category", listing.getItemCategory() != null ? listing.getItemCategory().getName() : "",
                            "manufacturer", listing.getManufacturer() != null ? listing.getManufacturer().getName() : "",
                            "part_number", listing.getPartNumber() != null ? listing.getPartNumber() : "",
                            "quantity", listing.getQuantity() != null ? listing.getQuantity() : "",
                            "unit", listing.getUnit() != null ? listing.getUnit().getName() : "",
                            "price", listing.getPrice() != null ? listing.getPrice() : "",
                            "condition", listing.getCondition() != null ? listing.getCondition().getName() : ""
                    ))
            );
            return objectMapper.writeValueAsString(current);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void applyExtraction(Listing listing, ExtractionResult result) {
        if (result.getIntent() != null) {
            try {
                listing.setIntent(IntentType.valueOf(result.getIntent().toLowerCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid intent '{}' from retry extraction; keeping original", result.getIntent());
            }
        }
        listing.setConfidenceScore(result.getConfidence());

        if (result.getItems() != null && !result.getItems().isEmpty()) {
            ExtractionResult.ExtractedItem item = result.getItems().get(0);

            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                listing.setItemDescription(item.getDescription().trim());
            }
            if (item.getCategory() != null && !item.getCategory().isBlank()) {
                categoryRepository.findByNameIgnoreCase(item.getCategory().trim())
                        .ifPresent(listing::setItemCategory);
            }
            if (item.getManufacturer() != null && !item.getManufacturer().isBlank()) {
                manufacturerRepository.findByNameIgnoreCase(item.getManufacturer().trim())
                        .ifPresent(listing::setManufacturer);
            }
            if (item.getPartNumber() != null) {
                listing.setPartNumber(item.getPartNumber().trim());
            }
            if (item.getQuantity() != null) {
                listing.setQuantity(BigDecimal.valueOf(item.getQuantity()));
            }
            if (item.getUnit() != null && !item.getUnit().isBlank()) {
                unitRepository.findByNameIgnoreCase(item.getUnit().trim())
                        .or(() -> unitRepository.findByAbbreviationIgnoreCase(item.getUnit().trim()))
                        .ifPresent(listing::setUnit);
            }
            if (item.getPrice() != null) {
                listing.setPrice(BigDecimal.valueOf(item.getPrice()));
            }
            if (item.getCurrency() != null && !item.getCurrency().isBlank()) {
                listing.setPriceCurrency(item.getCurrency().trim().toUpperCase());
            }
            if (item.getCondition() != null && !item.getCondition().isBlank()) {
                conditionRepository.findByNameIgnoreCase(item.getCondition().trim())
                        .ifPresent(listing::setCondition);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cross-post detection
    // -------------------------------------------------------------------------

    /**
     * Enriches a list of DTOs with cross-post counts in a single batch query.
     * Non-fatal: if the query fails the DTOs are returned unchanged.
     */
    public void enrichWithCrossPostCounts(List<ListingDTO> dtos) {
        try {
            List<UUID> ids = dtos.stream().map(ListingDTO::getId).collect(Collectors.toList());
            if (ids.isEmpty()) return;

            Map<UUID, Integer> counts = listingRepository.countCrossPostsForListings(ids)
                    .stream()
                    .collect(Collectors.toMap(
                            ListingRepository.CrossPostCountProjection::getListingId,
                            ListingRepository.CrossPostCountProjection::getCrossPostCount
                    ));

            for (ListingDTO dto : dtos) {
                dto.setCrossPostCount(counts.getOrDefault(dto.getId(), 0));
            }
        } catch (Exception e) {
            log.warn("Cross-post count enrichment failed (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Returns the cross-posts of a given listing (same sender + part# + price,
     * different raw message).
     */
    @Transactional(readOnly = true)
    public List<CrossPostDTO> getCrossPosts(UUID id) {
        List<Listing> crossPosts = listingRepository.findCrossPostsOf(id);
        return crossPosts.stream()
                .map(CrossPostDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Exchange rate helpers
    // -------------------------------------------------------------------------

    private void recalculateExchangeRate(Listing listing) {
        try {
            if (listing.getPrice() != null && listing.getPriceCurrency() != null) {
                LocalDate rateDate = listing.getCreatedAt() != null
                        ? listing.getCreatedAt().toLocalDate()
                        : LocalDate.now();
                BigDecimal rate = exchangeRateService.getRateToUsd(listing.getPriceCurrency(), rateDate);
                if (rate != null) {
                    listing.setExchangeRateToUsd(rate);
                    listing.setPriceUsd(exchangeRateService.computeUsdPrice(
                            listing.getPrice(), listing.getPriceCurrency(), rateDate));
                }
            } else {
                listing.setExchangeRateToUsd(null);
                listing.setPriceUsd(null);
            }
        } catch (Exception e) {
            log.warn("Exchange rate recalculation failed for listing {} (non-fatal): {}",
                    listing.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Backfill exchange rates (admin)
    // -------------------------------------------------------------------------

    /**
     * Backfills exchange rates for listings that have a price but no exchange rate.
     *
     * @return the number of listings updated
     */
    public int backfillExchangeRates() {
        List<Listing> listings = listingRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.isNotNull(root.get("price")),
                        cb.isNull(root.get("exchangeRateToUsd"))
                )
        );

        int updated = 0;
        for (Listing listing : listings) {
            try {
                LocalDate rateDate = listing.getCreatedAt() != null
                        ? listing.getCreatedAt().toLocalDate()
                        : LocalDate.now();
                BigDecimal rate = exchangeRateService.getRateToUsd(listing.getPriceCurrency(), rateDate);
                if (rate != null) {
                    listing.setExchangeRateToUsd(rate);
                    listing.setPriceUsd(exchangeRateService.computeUsdPrice(
                            listing.getPrice(), listing.getPriceCurrency(), rateDate));
                    listingRepository.save(listing);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("Backfill failed for listing {}: {}", listing.getId(), e.getMessage());
            }
        }

        log.info("Backfilled exchange rates for {} listings", updated);
        return updated;
    }

    // -------------------------------------------------------------------------
    // Scheduled: listing expiry
    // -------------------------------------------------------------------------

    /**
     * Periodically expires active listings whose {@code expiresAt} has passed.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void expireListings() {
        int expired = listingRepository.expireListingsBefore(OffsetDateTime.now());
        if (expired > 0) {
            log.info("Expired {} listings that passed their expiry date", expired);
        }
    }
}
