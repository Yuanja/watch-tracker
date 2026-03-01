package com.tradeintel.processing;

import com.tradeintel.common.entity.ReviewQueueItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ReviewQueueItem} entities.
 *
 * <p>Provides query methods used by the review queue admin API and the
 * message processing pipeline for creating review items.</p>
 */
@Repository
public interface ReviewQueueItemRepository extends JpaRepository<ReviewQueueItem, UUID> {

    /**
     * Returns a page of review queue items with the given status, ordered by
     * creation time ascending (oldest items first for FIFO processing).
     *
     * @param status   the review status to filter by (e.g. "pending", "resolved", "skipped")
     * @param pageable pagination and sorting parameters
     * @return page of matching review queue items
     */
    Page<ReviewQueueItem> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    /**
     * Returns a page of review queue items filtered by status.
     *
     * @param status   the review status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of matching review queue items
     */
    Page<ReviewQueueItem> findByStatus(String status, Pageable pageable);

    /**
     * Counts review queue items with the given status.
     * Used for dashboard metrics and queue size indicators.
     *
     * @param status the review status to count
     * @return total count of items with the given status
     */
    long countByStatus(String status);

    /**
     * Returns all review queue items associated with a specific listing.
     *
     * @param listingId the listing UUID
     * @return list of review items for the listing
     */
    List<ReviewQueueItem> findByListingId(UUID listingId);

    @Modifying
    @Query("DELETE FROM ReviewQueueItem r WHERE r.listing.id IN :listingIds")
    int deleteByListingIdIn(@Param("listingIds") List<UUID> listingIds);
}
