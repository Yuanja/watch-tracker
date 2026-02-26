package com.tradeintel.processing;

import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.NotificationRule;
import com.tradeintel.notification.NotificationDispatcher;
import com.tradeintel.notification.NotificationRuleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Matches new active listings against user-defined notification rules.
 *
 * <p>Each active rule is evaluated against the listing's fields:
 * <ul>
 *   <li><b>Intent match</b> — if the rule specifies a parsed intent, the listing's
 *       intent must match.</li>
 *   <li><b>Keyword match</b> — if the rule has parsed keywords, at least one keyword
 *       must appear (case-insensitive) in the listing's description.</li>
 *   <li><b>Category match</b> — if the rule has parsed category IDs, the listing's
 *       category must be one of them.</li>
 *   <li><b>Price range match</b> — if the rule specifies price bounds, the listing's
 *       price must fall within the range.</li>
 * </ul>
 *
 * <p>All specified criteria must match (AND logic). Criteria that are null/empty
 * on the rule are considered to match any listing.</p>
 *
 * <p>When a match is found, the {@link NotificationDispatcher} is invoked to send
 * an email notification to the rule's owner.</p>
 */
@Service
public class NotificationMatcher {

    private static final Logger log = LogManager.getLogger(NotificationMatcher.class);

    private final NotificationRuleRepository notificationRuleRepository;
    private final NotificationDispatcher notificationDispatcher;

    public NotificationMatcher(NotificationRuleRepository notificationRuleRepository,
                               NotificationDispatcher notificationDispatcher) {
        this.notificationRuleRepository = notificationRuleRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    /**
     * Evaluates the given listing against all active notification rules and
     * dispatches email notifications for matches.
     *
     * @param listing the newly created active listing to match against rules
     */
    @Transactional(readOnly = true)
    public void matchAndDispatch(Listing listing) {
        if (listing == null) {
            return;
        }

        List<NotificationRule> activeRules = notificationRuleRepository
                .findByIsActiveTrueAndUserIsActiveTrue();

        if (activeRules.isEmpty()) {
            log.debug("No active notification rules to match against listing {}", listing.getId());
            return;
        }

        int matchCount = 0;

        for (NotificationRule rule : activeRules) {
            if (matches(rule, listing)) {
                matchCount++;
                log.info("Notification rule {} (user={}) matched listing {} - '{}'",
                        rule.getId(),
                        rule.getUser().getId(),
                        listing.getId(),
                        listing.getItemDescription().length() > 80
                                ? listing.getItemDescription().substring(0, 80) + "..."
                                : listing.getItemDescription());

                notificationDispatcher.dispatch(rule, listing);
            }
        }

        log.debug("Matched {} notification rules for listing {}", matchCount, listing.getId());
    }

    // -------------------------------------------------------------------------
    // Private matching logic
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the listing satisfies all non-null criteria on the rule.
     */
    private boolean matches(NotificationRule rule, Listing listing) {
        // Intent match
        if (rule.getParsedIntent() != null && listing.getIntent() != rule.getParsedIntent()) {
            return false;
        }

        // Keyword match: at least one keyword must appear in the description
        if (rule.getParsedKeywords() != null && rule.getParsedKeywords().length > 0) {
            String descLower = listing.getItemDescription().toLowerCase();
            boolean anyKeywordMatch = Arrays.stream(rule.getParsedKeywords())
                    .anyMatch(keyword -> keyword != null
                            && descLower.contains(keyword.toLowerCase()));
            if (!anyKeywordMatch) {
                return false;
            }
        }

        // Category match: listing category must be in the rule's category list
        if (rule.getParsedCategoryIds() != null && rule.getParsedCategoryIds().length > 0) {
            if (listing.getItemCategory() == null) {
                return false;
            }
            UUID listingCategoryId = listing.getItemCategory().getId();
            boolean categoryMatch = Arrays.asList(rule.getParsedCategoryIds())
                    .contains(listingCategoryId);
            if (!categoryMatch) {
                return false;
            }
        }

        // Price range match
        BigDecimal listingPrice = listing.getPrice();
        if (rule.getParsedPriceMin() != null) {
            if (listingPrice == null || listingPrice.compareTo(rule.getParsedPriceMin()) < 0) {
                return false;
            }
        }
        if (rule.getParsedPriceMax() != null) {
            if (listingPrice == null || listingPrice.compareTo(rule.getParsedPriceMax()) > 0) {
                return false;
            }
        }

        return true;
    }
}
