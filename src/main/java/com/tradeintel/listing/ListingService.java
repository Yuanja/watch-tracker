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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final EntityManager entityManager;
    private final AuditService auditService;

    public ListingService(ListingRepository listingRepository,
                          CategoryRepository categoryRepository,
                          EntityManager entityManager,
                          AuditService auditService) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
        this.auditService = auditService;
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
