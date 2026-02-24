package com.tradeintel.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.listing.ListingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chat tool that returns aggregate market statistics about listings.
 *
 * <p>Provides counts by intent (sell/want), by status, and total active
 * listings. Useful for giving the user an overview of market activity.</p>
 */
@Component
public class MarketStatsTool {

    private static final Logger log = LogManager.getLogger(MarketStatsTool.class);

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;

    public MarketStatsTool(ListingRepository listingRepository, ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the market_stats tool.
     *
     * @param params tool parameters (currently unused but kept for interface consistency)
     * @return JSON string with market statistics
     */
    public String execute(Map<String, Object> params) {
        try {
            long sellCount = listingRepository.countByIntent(IntentType.sell);
            long wantCount = listingRepository.countByIntent(IntentType.want);
            long unknownCount = listingRepository.countByIntent(IntentType.unknown);

            long activeCount = listingRepository.countByStatusAndNotDeleted(ListingStatus.active);
            long expiredCount = listingRepository.countByStatus(ListingStatus.expired);
            long pendingReviewCount = listingRepository.countByStatus(ListingStatus.pending_review);

            long totalListings = listingRepository.count();

            ObjectNode stats = objectMapper.createObjectNode();
            stats.put("totalListings", totalListings);
            stats.put("activeListings", activeCount);

            ObjectNode byIntent = stats.putObject("byIntent");
            byIntent.put("sell", sellCount);
            byIntent.put("want", wantCount);
            byIntent.put("unknown", unknownCount);

            ObjectNode byStatus = stats.putObject("byStatus");
            byStatus.put("active", activeCount);
            byStatus.put("expired", expiredCount);
            byStatus.put("pending_review", pendingReviewCount);

            String json = objectMapper.writeValueAsString(stats);
            log.info("MarketStatsTool: total={}, active={}, sell={}, want={}",
                    totalListings, activeCount, sellCount, wantCount);
            return json;

        } catch (Exception e) {
            log.error("MarketStatsTool failed", e);
            return "{\"error\": \"Failed to retrieve market stats: " + e.getMessage() + "\"}";
        }
    }
}
