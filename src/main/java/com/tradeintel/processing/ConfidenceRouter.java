package com.tradeintel.processing;

import com.tradeintel.archive.EmbeddingService;
import com.tradeintel.common.entity.Category;
import com.tradeintel.common.entity.Condition;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.Manufacturer;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.Unit;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.normalize.ConditionRepository;
import com.tradeintel.normalize.ManufacturerRepository;
import com.tradeintel.normalize.UnitRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Routes extracted listing items based on the LLM confidence score.
 *
 * <p>Confidence routing thresholds (configurable via application properties):
 * <ul>
 *   <li><b>confidence &ge; auto threshold (0.8)</b> — listing is auto-accepted
 *       with {@code status=active}, {@code needsHumanReview=false}</li>
 *   <li><b>review threshold (0.5) &le; confidence &lt; auto threshold</b> — listing is
 *       created with {@code status=pending_review}, {@code needsHumanReview=true}</li>
 *   <li><b>confidence &lt; review threshold</b> — item is discarded (no listing created)</li>
 * </ul>
 *
 * <p>For each created listing, category, manufacturer, unit, and condition are
 * resolved by case-insensitive name lookup against the admin-managed reference
 * tables. If no match is found the FK is left null on the listing.</p>
 */
@Service
public class ConfidenceRouter {

    private static final Logger log = LogManager.getLogger(ConfidenceRouter.class);

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final UnitRepository unitRepository;
    private final ConditionRepository conditionRepository;
    private final EmbeddingService embeddingService;
    private final ExchangeRateService exchangeRateService;

    private final double autoThreshold;
    private final double reviewThreshold;
    private final int expiryDays;

    public ConfidenceRouter(ListingRepository listingRepository,
                            CategoryRepository categoryRepository,
                            ManufacturerRepository manufacturerRepository,
                            UnitRepository unitRepository,
                            ConditionRepository conditionRepository,
                            EmbeddingService embeddingService,
                            ExchangeRateService exchangeRateService,
                            @Value("${app.processing.confidence-auto-threshold}") double autoThreshold,
                            @Value("${app.processing.confidence-review-threshold}") double reviewThreshold,
                            @Value("${app.processing.listing-expiry-days}") int expiryDays) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.unitRepository = unitRepository;
        this.conditionRepository = conditionRepository;
        this.embeddingService = embeddingService;
        this.exchangeRateService = exchangeRateService;
        this.autoThreshold = autoThreshold;
        this.reviewThreshold = reviewThreshold;
        this.expiryDays = expiryDays;
    }

    /**
     * Creates listings from the extraction result and routes them based on confidence.
     *
     * @param result the LLM extraction result
     * @param msg    the raw WhatsApp message being processed
     * @return list of created (and persisted) listings; may be empty if all items
     *         were below the review threshold
     */
    @Transactional
    public List<Listing> route(ExtractionResult result, RawMessage msg) {
        List<Listing> created = new ArrayList<>();
        double confidence = result.getConfidence();

        if (confidence < reviewThreshold) {
            log.debug("Confidence {} below review threshold {}; discarding all items from message {}",
                    confidence, reviewThreshold, msg.getId());
            return created;
        }

        IntentType intentType = resolveIntent(result.getIntent());

        for (ExtractionResult.ExtractedItem item : result.getItems()) {
            Listing listing = new Listing();

            // Provenance
            listing.setRawMessage(msg);
            listing.setGroup(msg.getGroup());

            // Classification
            listing.setIntent(intentType);
            listing.setConfidenceScore(confidence);

            // Description
            String description = item.getDescription();
            if (description == null || description.isBlank()) {
                description = msg.getMessageBody() != null
                        ? msg.getMessageBody().substring(0, Math.min(msg.getMessageBody().length(), 500))
                        : "No description available";
            }
            listing.setItemDescription(description);

            // Original content
            listing.setOriginalText(msg.getMessageBody() != null ? msg.getMessageBody() : "");
            listing.setSenderName(msg.getSenderName());
            listing.setSenderPhone(msg.getSenderPhone());

            // Normalized field lookups (case-insensitive, null-safe)
            listing.setItemCategory(resolveCategory(item.getCategory()));
            listing.setManufacturer(resolveManufacturer(item.getManufacturer()));
            listing.setUnit(resolveUnit(item.getUnit()));
            listing.setCondition(resolveCondition(item.getCondition()));

            // Part number and model name
            listing.setPartNumber(item.getPartNumber());
            listing.setModelName(item.getModelName());

            // Watch-specific detail fields
            listing.setDialColor(item.getDialColor());
            listing.setCaseMaterial(item.getCaseMaterial());
            listing.setYear(item.getYear());
            listing.setCaseSizeMm(item.getCaseSizeMm());
            listing.setSetComposition(item.getSetComposition());
            listing.setBraceletStrap(item.getBraceletStrap());

            // Quantity
            if (item.getQuantity() != null) {
                listing.setQuantity(BigDecimal.valueOf(item.getQuantity()));
            }

            // Price and currency
            if (item.getPrice() != null) {
                listing.setPrice(BigDecimal.valueOf(item.getPrice()));
            }
            if (item.getCurrency() != null && !item.getCurrency().isBlank()) {
                listing.setPriceCurrency(item.getCurrency().trim().toUpperCase());
            }

            // Materialize exchange rate and USD price
            try {
                if (listing.getPrice() != null && listing.getPriceCurrency() != null) {
                    LocalDate rateDate = LocalDate.now();
                    BigDecimal rate = exchangeRateService.getRateToUsd(listing.getPriceCurrency(), rateDate);
                    if (rate != null) {
                        listing.setExchangeRateToUsd(rate);
                        listing.setPriceUsd(exchangeRateService.computeUsdPrice(
                                listing.getPrice(), listing.getPriceCurrency(), rateDate));
                    }
                }
            } catch (Exception e) {
                log.warn("Exchange rate computation failed for listing (non-fatal): {}", e.getMessage());
            }

            // Confidence routing
            if (confidence >= autoThreshold) {
                listing.setStatus(ListingStatus.active);
                listing.setNeedsHumanReview(false);
                log.debug("Auto-accepted listing with confidence {}", confidence);
            } else {
                listing.setStatus(ListingStatus.pending_review);
                listing.setNeedsHumanReview(true);
                log.debug("Listing queued for review with confidence {}", confidence);
            }

            // Expiry
            listing.setExpiresAt(OffsetDateTime.now().plusDays(expiryDays));

            Listing saved = listingRepository.save(listing);

            // Generate embedding for semantic search
            try {
                float[] embedding = embeddingService.embed(description);
                if (embedding != null) {
                    saved.setEmbedding(embedding);
                    listingRepository.save(saved);
                }
            } catch (Exception e) {
                log.warn("Embedding generation failed for listing {} (non-fatal): {}",
                        saved.getId(), e.getMessage());
            }

            created.add(saved);
        }

        log.info("Routed {} listings from message {} (confidence={})",
                created.size(), msg.getId(), confidence);
        return created;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private IntentType resolveIntent(String intent) {
        if (intent == null) {
            return IntentType.unknown;
        }
        try {
            return IntentType.valueOf(intent.toLowerCase().trim());
        } catch (IllegalArgumentException e) {
            log.debug("Unknown intent '{}'; defaulting to unknown", intent);
            return IntentType.unknown;
        }
    }

    private Category resolveCategory(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return categoryRepository.findByNameIgnoreCase(name.trim()).orElse(null);
    }

    private Manufacturer resolveManufacturer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        // Try canonical name first
        java.util.Optional<Manufacturer> byName = manufacturerRepository.findByNameIgnoreCase(trimmed);
        if (byName.isPresent()) {
            return byName.get();
        }
        // Fallback: search aliases in-memory (avoids PostgreSQL-specific native query
        // that would fail in H2 test environments and poison the transaction)
        List<Manufacturer> active = manufacturerRepository.findByIsActiveTrueOrderByNameAsc();
        return active.stream()
                .filter(m -> {
                    String[] aliases = m.getAliases();
                    if (aliases == null) return false;
                    for (String alias : aliases) {
                        if (alias.equalsIgnoreCase(trimmed)) return true;
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    private Unit resolveUnit(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // Try matching by name first, then by abbreviation
        return unitRepository.findByNameIgnoreCase(name.trim())
                .or(() -> unitRepository.findByAbbreviationIgnoreCase(name.trim()))
                .orElse(null);
    }

    private Condition resolveCondition(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        // Tier 1: exact name match
        return conditionRepository.findByNameIgnoreCase(trimmed)
                // Tier 2: abbreviation match (handles "new", "BNIB", "pre-owned", etc.)
                .or(() -> conditionRepository.findByAbbreviationIgnoreCase(trimmed))
                // Tier 3: partial name match (handles "unworn" matching "New / Unworn")
                .or(() -> {
                    List<Condition> all = conditionRepository.findByIsActiveTrueOrderBySortOrderAsc();
                    return all.stream()
                            .filter(c -> c.getName().toLowerCase().contains(trimmed.toLowerCase()))
                            .findFirst();
                })
                .orElse(null);
    }
}
