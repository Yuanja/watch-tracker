package com.tradeintel.notification;

import com.tradeintel.common.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link NotificationRule} entities.
 *
 * <p>Provides queries used by the notification system to list a user's rules
 * and to find all active rules across active users for matching against new
 * listings.</p>
 */
@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRule, UUID> {

    /**
     * Returns all notification rules belonging to the given user,
     * ordered by creation date descending.
     *
     * @param userId the user's UUID
     * @return list of notification rules
     */
    List<NotificationRule> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Returns only active notification rules belonging to the given user.
     *
     * @param userId the user's UUID
     * @return list of active notification rules
     */
    List<NotificationRule> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Returns all active notification rules for active users.
     * Used by the processing pipeline to match new listings against all rules.
     *
     * @return list of active rules belonging to active users
     */
    @Query("SELECT nr FROM NotificationRule nr WHERE nr.isActive = true AND nr.user.isActive = true")
    List<NotificationRule> findByIsActiveTrueAndUserIsActiveTrue();
}
