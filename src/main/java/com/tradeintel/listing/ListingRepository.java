package com.tradeintel.listing;

import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Listing} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic criteria queries
 * used by the filtered search endpoint. Semantic (pgvector) search will be added
 * in Phase 3 via native queries.</p>
 */
@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

    /**
     * Returns all non-deleted listings with the given status, paginated.
     *
     * @param status   the lifecycle status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of matching, non-soft-deleted listings
     */
    Page<Listing> findByStatusAndDeletedAtIsNull(ListingStatus status, Pageable pageable);

    /**
     * Counts listings by intent, regardless of status or soft-delete state.
     * Used for the stats aggregation endpoint.
     *
     * @param intent the intent type to count
     * @return total count
     */
    long countByIntent(IntentType intent);

    /**
     * Counts listings by status, regardless of soft-delete state.
     * Used for the stats aggregation endpoint.
     *
     * @param status the lifecycle status to count
     * @return total count
     */
    long countByStatus(ListingStatus status);

    /**
     * Counts only non-deleted listings by status.
     * Provides accurate active-listing counts that exclude soft-deleted records.
     *
     * @param status the lifecycle status to count
     * @return count of non-soft-deleted listings with the given status
     */
    @Query("SELECT COUNT(l) FROM Listing l WHERE l.status = :status AND l.deletedAt IS NULL")
    long countByStatusAndNotDeleted(ListingStatus status);
}
