package com.tradeintel.notification;

import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.NotificationRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Dispatches notifications to users when a listing matches one of their
 * active notification rules.
 *
 * <p>Currently a placeholder that logs the notification event. Email
 * integration will be added in a future iteration.</p>
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LogManager.getLogger(NotificationDispatcher.class);

    /**
     * Dispatches a notification for the given rule and matching listing.
     *
     * @param rule    the notification rule that was triggered
     * @param listing the listing that matches the rule
     */
    public void dispatch(NotificationRule rule, Listing listing) {
        log.info("Notification triggered: rule={}, listing={}, user={}, channel={}, email={}",
                rule.getId(),
                listing.getId(),
                rule.getUser().getId(),
                rule.getNotifyChannel(),
                rule.getNotifyEmail());

        // TODO: Implement email dispatch via Spring JavaMailSender
        // For now, this is a placeholder that logs the notification event.
    }
}
