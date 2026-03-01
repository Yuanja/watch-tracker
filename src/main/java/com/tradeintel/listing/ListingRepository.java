package com.tradeintel.listing;

import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Listing} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic criteria queries
 * used by the filtered search endpoint. Semantic (pgvector) search is supported
 * via native queries for cosine similarity ordering.</p>
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
    long countByStatusAndNotDeleted(@Param("status") ListingStatus status);

    /**
     * Returns non-deleted active listings that have embeddings, ordered by
     * keyword match on description or part number. Used as a fallback when
     * pgvector is not available (e.g., H2 in tests).
     */
    @Query(value = "SELECT l FROM Listing l WHERE l.deletedAt IS NULL " +
           "AND l.status = 'active' " +
           "AND LOWER(l.itemDescription) LIKE LOWER(CONCAT('%', :query, '%'))",
           countQuery = "SELECT COUNT(l) FROM Listing l WHERE l.deletedAt IS NULL " +
           "AND l.status = 'active' " +
           "AND LOWER(l.itemDescription) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Listing> findByDescriptionContaining(@Param("query") String query, Pageable pageable);

    /**
     * Bulk-updates active listings whose expiry date has passed to expired status.
     *
     * @param now the current timestamp
     * @return number of listings updated
     */
    @Modifying
    @Query("UPDATE Listing l SET l.status = 'expired' " +
           "WHERE l.status = 'active' AND l.expiresAt IS NOT NULL AND l.expiresAt < :now")
    int expireListingsBefore(@Param("now") OffsetDateTime now);

    @EntityGraph(attributePaths = {"manufacturer", "condition"})
    @Query("SELECT l FROM Listing l WHERE l.rawMessage.id IN :messageIds AND l.deletedAt IS NULL")
    List<Listing> findByRawMessageIdInAndDeletedAtIsNull(@Param("messageIds") List<UUID> messageIds);

    /**
     * Finds a non-deleted listing that was extracted from a raw message with the given whapi message ID.
     * Used to locate the original listing when a "sold" reply references it.
     */
    @EntityGraph(attributePaths = {"manufacturer", "condition"})
    @Query("SELECT l FROM Listing l WHERE l.rawMessage.whapiMsgId = :whapiMsgId AND l.deletedAt IS NULL")
    Optional<Listing> findByRawMessageWhapiMsgId(@Param("whapiMsgId") String whapiMsgId);

    @Query("SELECT l.id FROM Listing l WHERE l.status IN :statuses AND l.deletedAt IS NULL")
    List<UUID> findIdsByStatusIn(@Param("statuses") List<ListingStatus> statuses);

    @Modifying
    @Query("DELETE FROM Listing l WHERE l.status IN :statuses AND l.deletedAt IS NULL")
    int deleteByStatusIn(@Param("statuses") List<ListingStatus> statuses);

    // -------------------------------------------------------------------------
    // Cross-post detection
    // -------------------------------------------------------------------------

    interface CrossPostCountProjection {
        UUID getListingId();
        int getCrossPostCount();
    }

    @Query(value = """
        SELECT l.id AS listingId, COUNT(other.id) AS crossPostCount
        FROM   listings l
        JOIN   listings other
          ON   other.id != l.id
          AND  other.raw_message_id != l.raw_message_id
          AND  other.group_id != l.group_id
          AND  other.deleted_at IS NULL
          AND  other.part_number = l.part_number
          AND  (
               (other.sender_name IS NOT NULL AND l.sender_name IS NOT NULL AND other.sender_name = l.sender_name)
               OR (other.sender_phone IS NOT NULL AND l.sender_phone IS NOT NULL AND other.sender_phone = l.sender_phone)
              )
        WHERE  l.id IN :listingIds AND l.deleted_at IS NULL
          AND  l.part_number IS NOT NULL
        GROUP BY l.id
        """, nativeQuery = true)
    List<CrossPostCountProjection> countCrossPostsForListings(@Param("listingIds") List<UUID> listingIds);

    @Query(value = """
        SELECT other.* FROM listings other
        JOIN listings l ON l.id = :listingId
        WHERE other.id != l.id AND other.raw_message_id != l.raw_message_id
          AND other.group_id != l.group_id
          AND other.deleted_at IS NULL
          AND other.part_number = l.part_number
          AND ((other.sender_name IS NOT NULL AND l.sender_name IS NOT NULL AND other.sender_name = l.sender_name)
               OR (other.sender_phone IS NOT NULL AND l.sender_phone IS NOT NULL AND other.sender_phone = l.sender_phone))
        ORDER BY other.created_at DESC
        """, nativeQuery = true)
    List<Listing> findCrossPostsOf(@Param("listingId") UUID listingId);
}
