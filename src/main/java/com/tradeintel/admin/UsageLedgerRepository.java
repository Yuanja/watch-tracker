package com.tradeintel.admin;

import com.tradeintel.common.entity.UsageLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UsageLedger} entities.
 *
 * <p>Provides queries needed for per-user cost views and the uber_admin
 * cost report that aggregates spending across all users within a date range.
 */
@Repository
public interface UsageLedgerRepository extends JpaRepository<UsageLedger, UUID> {

    /**
     * Returns a page of daily usage records for the given user, newest first.
     *
     * @param userId   the user's UUID
     * @param pageable pagination and sorting parameters
     * @return page of usage ledger entries ordered by {@code period_date} descending
     */
    Page<UsageLedger> findByUserIdOrderByPeriodDateDesc(UUID userId, Pageable pageable);

    /**
     * Returns all daily usage records for the given user within the specified
     * inclusive date range, ordered by date ascending.
     *
     * @param userId    the user's UUID
     * @param startDate start of the period (inclusive)
     * @param endDate   end of the period (inclusive)
     * @return list of matching usage ledger entries
     */
    @Query("SELECT ul FROM UsageLedger ul WHERE ul.user.id = :userId " +
           "AND ul.periodDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ul.periodDate ASC")
    List<UsageLedger> findByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Returns all daily usage records within the specified inclusive date range
     * across all users, ordered by user id and then date ascending. Used for
     * the uber_admin cost report CSV export.
     *
     * @param startDate start of the period (inclusive)
     * @param endDate   end of the period (inclusive)
     * @return list of matching usage ledger entries for all users
     */
    @Query("SELECT ul FROM UsageLedger ul WHERE ul.periodDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ul.user.id ASC, ul.periodDate ASC")
    List<UsageLedger> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
