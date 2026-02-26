package com.tradeintel.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.listing.ListingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Chat tool that retrieves full details for a single listing by ID.
 *
 * <p>Returns all structured fields (description, category, manufacturer,
 * price, condition, seller, original text, etc.) formatted as a JSON
 * object suitable for inclusion in an LLM context.</p>
 */
@Component
public class GetListingDetailsTool {

    private static final Logger log = LogManager.getLogger(GetListingDetailsTool.class);

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;

    public GetListingDetailsTool(ListingRepository listingRepository, ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the get_listing_details tool with the given parameters.
     *
     * @param params map containing "id" — the listing UUID string
     * @return JSON string with listing details, or an error message
     */
    public String execute(Map<String, Object> params) {
        try {
            String idStr = params.get("id") != null ? params.get("id").toString() : null;
            if (idStr == null || idStr.isBlank()) {
                return "{\"error\": \"Missing required parameter: id\"}";
            }

            UUID id = UUID.fromString(idStr);
            Optional<Listing> optListing = listingRepository.findById(id);

            if (optListing.isEmpty()) {
                return "{\"error\": \"Listing not found: " + idStr + "\"}";
            }

            Listing listing = optListing.get();
            ObjectNode node = objectMapper.createObjectNode();

            node.put("id", listing.getId().toString());
            node.put("intent", listing.getIntent().name());
            node.put("status", listing.getStatus().name());
            node.put("description", listing.getItemDescription());

            if (listing.getOriginalText() != null) {
                node.put("originalText", listing.getOriginalText());
            }
            if (listing.getConfidenceScore() != null) {
                node.put("confidenceScore", listing.getConfidenceScore());
            }
            if (listing.getItemCategory() != null) {
                node.put("category", listing.getItemCategory().getName());
            }
            if (listing.getManufacturer() != null) {
                node.put("manufacturer", listing.getManufacturer().getName());
            }
            if (listing.getPartNumber() != null) {
                node.put("partNumber", listing.getPartNumber());
            }
            if (listing.getQuantity() != null) {
                node.put("quantity", listing.getQuantity().doubleValue());
            }
            if (listing.getUnit() != null) {
                node.put("unit", listing.getUnit().getName());
            }
            if (listing.getPrice() != null) {
                node.put("price", listing.getPrice().doubleValue());
                node.put("currency", listing.getPriceCurrency());
            }
            if (listing.getCondition() != null) {
                node.put("condition", listing.getCondition().getName());
            }
            if (listing.getSenderName() != null) {
                node.put("seller", listing.getSenderName());
            }
            if (listing.getSenderPhone() != null) {
                node.put("sellerPhone", listing.getSenderPhone());
            }
            if (listing.getGroup() != null) {
                node.put("groupName", listing.getGroup().getGroupName());
            }
            if (listing.getCreatedAt() != null) {
                node.put("createdAt", listing.getCreatedAt().toString());
            }

            String json = objectMapper.writeValueAsString(node);
            log.info("GetListingDetailsTool returned details for listing {}", id);
            return json;

        } catch (IllegalArgumentException e) {
            log.warn("GetListingDetailsTool invalid UUID: {}", e.getMessage());
            return "{\"error\": \"Invalid listing ID format\"}";
        } catch (Exception e) {
            log.error("GetListingDetailsTool failed", e);
            return "{\"error\": \"Failed to get listing details: " + e.getMessage() + "\"}";
        }
    }
}
