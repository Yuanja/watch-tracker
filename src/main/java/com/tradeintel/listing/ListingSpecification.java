package com.tradeintel.listing;

import com.tradeintel.common.entity.Listing;
import com.tradeintel.listing.dto.ListingSearchRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for building {@link Specification} instances used by the
 * listing search endpoint.
 *
 * <p>All predicates are combined with AND semantics. Null filter values
 * are simply omitted so the query returns unrestricted results for that
 * dimension. Soft-deleted listings ({@code deleted_at IS NOT NULL}) are always
 * excluded from search results.</p>
 */
public final class ListingSpecification {

    private ListingSpecification() {
        // static factory only
    }

    /**
     * Builds a {@link Specification} from the given search request.
     *
     * <p>Applied predicates (when non-null):
     * <ul>
     *   <li>{@code deletedAt IS NULL} — always applied</li>
     *   <li>{@code intent = request.intent}</li>
     *   <li>{@code itemCategory.id = request.categoryId}</li>
     *   <li>{@code manufacturer.id = request.manufacturerId}</li>
     *   <li>{@code condition.id = request.conditionId}</li>
     *   <li>{@code price >= request.priceMin}</li>
     *   <li>{@code price <= request.priceMax}</li>
     *   <li>{@code createdAt >= request.createdAfter}</li>
     *   <li>{@code createdAt <= request.createdBefore}</li>
     *   <li>{@code status = request.status} (defaults to active if null)</li>
     *   <li>keyword LIKE on {@code itemDescription} or {@code partNumber}</li>
     * </ul>
     *
     * @param request the search parameters; must not be null
     * @return a combined AND specification
     */
    public static Specification<Listing> from(ListingSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted listings
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Intent filter
            if (request.getIntent() != null) {
                predicates.add(cb.equal(root.get("intent"), request.getIntent()));
            }

            // Category filter via join
            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(
                        root.get("itemCategory").get("id"), request.getCategoryId()));
            }

            // Manufacturer filter via join
            if (request.getManufacturerId() != null) {
                predicates.add(cb.equal(
                        root.get("manufacturer").get("id"), request.getManufacturerId()));
            }

            // Condition filter via join
            if (request.getConditionId() != null) {
                predicates.add(cb.equal(
                        root.get("condition").get("id"), request.getConditionId()));
            }

            // Price range
            if (request.getPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("price"), request.getPriceMin()));
            }
            if (request.getPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("price"), request.getPriceMax()));
            }

            // Date range on createdAt
            if (request.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), request.getCreatedAfter()));
            }
            if (request.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), request.getCreatedBefore()));
            }

            // Status filter — when specified, filter to that status;
            // when omitted, show all non-deleted statuses (active, sold, pending_review, expired)
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            } else {
                predicates.add(root.get("status").in(
                        com.tradeintel.common.entity.ListingStatus.active,
                        com.tradeintel.common.entity.ListingStatus.sold,
                        com.tradeintel.common.entity.ListingStatus.pending_review,
                        com.tradeintel.common.entity.ListingStatus.expired));
            }

            // Keyword search: case-insensitive LIKE on description, part number, and model name
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                String pattern = "%" + request.getQuery().trim().toLowerCase() + "%";
                Predicate descMatch = cb.like(
                        cb.lower(root.get("itemDescription")), pattern);
                Predicate partMatch = cb.like(
                        cb.lower(root.get("partNumber")), pattern);
                Predicate modelMatch = cb.like(
                        cb.lower(root.get("modelName")), pattern);
                predicates.add(cb.or(descMatch, partMatch, modelMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
