package com.tradeintel.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.listing.ListingSpecification;
import com.tradeintel.listing.dto.ListingSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chat tool that searches extracted listings by keyword, category,
 * manufacturer, intent, and price range.
 *
 * <p>Returns up to 10 matching listings formatted as a JSON array
 * suitable for inclusion in an LLM context.</p>
 */
@Component
public class SearchListingsTool {

    private static final Logger log = LogManager.getLogger(SearchListingsTool.class);
    private static final int MAX_RESULTS = 10;

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;

    public SearchListingsTool(ListingRepository listingRepository, ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the search_listings tool with the given parameters.
     *
     * @param params map of parameter names to values extracted from the LLM tool call
     * @return JSON string with up to 10 matching listings
     */
    public String execute(Map<String, Object> params) {
        try {
            ListingSearchRequest searchRequest = new ListingSearchRequest();

            if (params.containsKey("query") && params.get("query") != null) {
                searchRequest.setQuery(params.get("query").toString());
            }

            if (params.containsKey("intent") && params.get("intent") != null) {
                String intentStr = params.get("intent").toString().toLowerCase();
                try {
                    searchRequest.setIntent(IntentType.valueOf(intentStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid intent value in tool call: {}", intentStr);
                }
            }

            if (params.containsKey("priceMin") && params.get("priceMin") != null) {
                searchRequest.setPriceMin(new java.math.BigDecimal(params.get("priceMin").toString()));
            }

            if (params.containsKey("priceMax") && params.get("priceMax") != null) {
                searchRequest.setPriceMax(new java.math.BigDecimal(params.get("priceMax").toString()));
            }

            Page<Listing> results = listingRepository.findAll(
                    ListingSpecification.from(searchRequest),
                    PageRequest.of(0, MAX_RESULTS));

            ArrayNode resultsArray = objectMapper.createArrayNode();
            for (Listing listing : results.getContent()) {
                ObjectNode node = resultsArray.addObject();
                node.put("id", listing.getId().toString());
                node.put("description", listing.getItemDescription());
                node.put("intent", listing.getIntent().name());
                node.put("status", listing.getStatus().name());

                if (listing.getPrice() != null) {
                    node.put("price", listing.getPrice().doubleValue());
                    node.put("currency", listing.getPriceCurrency());
                }
                if (listing.getManufacturer() != null) {
                    node.put("manufacturer", listing.getManufacturer().getName());
                }
                if (listing.getItemCategory() != null) {
                    node.put("category", listing.getItemCategory().getName());
                }
                if (listing.getPartNumber() != null) {
                    node.put("partNumber", listing.getPartNumber());
                }
                if (listing.getSenderName() != null) {
                    node.put("seller", listing.getSenderName());
                }
            }

            String json = objectMapper.writeValueAsString(resultsArray);
            log.info("SearchListingsTool returned {} results for params={}", results.getNumberOfElements(), params);
            return json;

        } catch (Exception e) {
            log.error("SearchListingsTool failed", e);
            return "{\"error\": \"Search failed: " + e.getMessage() + "\"}";
        }
    }
}
