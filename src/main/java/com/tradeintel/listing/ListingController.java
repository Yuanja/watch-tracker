package com.tradeintel.listing;

import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.common.security.UberAdminOnly;
import com.tradeintel.listing.dto.ListingDTO;
import com.tradeintel.listing.dto.ListingSearchRequest;
import com.tradeintel.listing.dto.ListingStatsDTO;
import com.tradeintel.listing.dto.ListingUpdateRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST controller for the listings API.
 *
 * <p>Access control per endpoint:
 * <ul>
 *   <li>{@code GET /}    — authenticated (any role)</li>
 *   <li>{@code GET /{id}} — authenticated (any role)</li>
 *   <li>{@code GET /stats} — authenticated (any role)</li>
 *   <li>{@code PUT /{id}} — admin or uber_admin ({@link AdminOnly})</li>
 *   <li>{@code DELETE /{id}} — uber_admin only ({@link UberAdminOnly})</li>
 * </ul>
 *
 * <p>Listings with {@code deleted_at != null} are excluded from search results
 * but remain accessible via {@code GET /{id}} for audit purposes.
 */
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private static final Logger log = LogManager.getLogger(ListingController.class);

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    // -------------------------------------------------------------------------
    // GET /api/listings  — search/filter
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated, filtered list of non-deleted listings.
     *
     * <p>All query parameters are optional. The default status filter is
     * {@code active}; pass {@code status=pending_review} etc. to override.
     * Keyword search ({@code q}) performs a case-insensitive LIKE match on
     * {@code item_description} and {@code part_number}.
     *
     * @param intent          filter by intent
     * @param categoryId      filter by category UUID
     * @param manufacturerId  filter by manufacturer UUID
     * @param conditionId     filter by condition UUID
     * @param priceMin        minimum price (inclusive)
     * @param priceMax        maximum price (inclusive)
     * @param createdAfter    earliest creation timestamp (inclusive)
     * @param createdBefore   latest creation timestamp (inclusive)
     * @param status          lifecycle status filter
     * @param q               keyword search on description and part number
     * @param page            zero-based page index (default 0)
     * @param size            page size (default 50, max 200)
     * @return page of {@link ListingDTO}
     */
    @GetMapping
    public ResponseEntity<Page<ListingDTO>> list(
            @RequestParam(required = false) com.tradeintel.common.entity.IntentType intent,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID manufacturerId,
            @RequestParam(required = false) UUID conditionId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) OffsetDateTime createdAfter,
            @RequestParam(required = false) OffsetDateTime createdBefore,
            @RequestParam(required = false) com.tradeintel.common.entity.ListingStatus status,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("GET /api/listings page={} size={} intent={} q='{}'", page, size, intent, query);

        ListingSearchRequest request = new ListingSearchRequest();
        request.setIntent(intent);
        request.setCategoryId(categoryId);
        request.setManufacturerId(manufacturerId);
        request.setConditionId(conditionId);
        request.setPriceMin(priceMin);
        request.setPriceMax(priceMax);
        request.setCreatedAfter(createdAfter);
        request.setCreatedBefore(createdBefore);
        request.setStatus(status);
        request.setQuery(query);
        request.setPage(page);
        request.setSize(size);

        Page<ListingDTO> result = listingService.list(request);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // GET /api/listings/stats  — aggregated counts
    // -------------------------------------------------------------------------

    /**
     * Returns aggregated counts by intent and status.
     * Must be declared before {@code GET /{id}} to avoid Spring treating "stats"
     * as a UUID path variable.
     *
     * @return {@link ListingStatsDTO}
     */
    @GetMapping("/stats")
    public ResponseEntity<ListingStatsDTO> getStats() {
        log.debug("GET /api/listings/stats");
        return ResponseEntity.ok(listingService.getStats());
    }

    // -------------------------------------------------------------------------
    // GET /api/listings/{id}  — single listing
    // -------------------------------------------------------------------------

    /**
     * Returns a single listing by UUID.
     * Soft-deleted listings are included (returned with a non-null {@code deletedAt}).
     *
     * @param id the listing UUID
     * @return the listing DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> getById(@PathVariable UUID id) {
        log.debug("GET /api/listings/{}", id);
        ListingDTO dto = listingService.getById(id);
        return ResponseEntity.ok(dto);
    }

    // -------------------------------------------------------------------------
    // PUT /api/listings/{id}  — update (admin+)
    // -------------------------------------------------------------------------

    /**
     * Updates fields on an existing listing.
     * Only provided (non-null) fields are modified.
     *
     * @param id      the listing UUID
     * @param request partial update payload
     * @return the updated listing DTO
     */
    @AdminOnly
    @PutMapping("/{id}")
    public ResponseEntity<ListingDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ListingUpdateRequest request) {

        log.info("PUT /api/listings/{}", id);
        ListingDTO updated = listingService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/listings/{id}  — soft-delete (uber_admin only)
    // -------------------------------------------------------------------------

    /**
     * Soft-deletes a listing by setting {@code deleted_at} and {@code deleted_by}.
     * The record is retained in the database; it will be excluded from search results.
     *
     * @param id        the listing UUID
     * @param principal the authenticated uber_admin performing the deletion
     * @return 204 No Content
     */
    @UberAdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("DELETE /api/listings/{} by userId={}", id, principal.getUserId());
        listingService.softDelete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
