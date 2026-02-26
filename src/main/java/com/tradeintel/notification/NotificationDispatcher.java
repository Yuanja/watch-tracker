package com.tradeintel.notification;

import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.NotificationRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Dispatches email notifications to users when a listing matches one of their
 * active notification rules.
 *
 * <p>Sends a plain-text email via Spring's {@link JavaMailSender}. The email
 * contains the listing description, intent, price (if available), and the
 * matched rule text for context.</p>
 *
 * <p>Dispatch is performed asynchronously so the processing pipeline is not
 * blocked by SMTP round-trips. Failures are logged but do not propagate.</p>
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LogManager.getLogger(NotificationDispatcher.class);

    private final String fromAddress;
    private final JavaMailSender mailSender;
    private final NotificationRuleRepository ruleRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDispatcher(JavaMailSender mailSender,
                                  NotificationRuleRepository ruleRepository,
                                  SimpMessagingTemplate messagingTemplate,
                                  @Value("${app.mail.from-address:noreply@tradeintel.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.ruleRepository = ruleRepository;
        this.messagingTemplate = messagingTemplate;
        this.fromAddress = fromAddress;
    }

    /**
     * Dispatches a notification for the given rule and matching listing.
     *
     * @param rule    the notification rule that was triggered
     * @param listing the listing that matches the rule
     */
    public void dispatch(NotificationRule rule, Listing listing) {
        String email = rule.getNotifyEmail();
        if (email == null || email.isBlank()) {
            email = rule.getUser().getEmail();
        }

        log.info("Dispatching notification: rule={}, listing={}, user={}, email={}",
                rule.getId(), listing.getId(), rule.getUser().getId(), email);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject(buildSubject(listing));
            message.setText(buildBody(rule, listing));

            mailSender.send(message);

            // Update last triggered timestamp
            rule.setLastTriggered(OffsetDateTime.now());
            ruleRepository.save(rule);

            log.info("Notification email sent: rule={}, listing={}, to={}",
                    rule.getId(), listing.getId(), email);

            // Push real-time notification to the user via WebSocket
            try {
                String userId = rule.getUser().getId().toString();
                Map<String, Object> wsPayload = Map.of(
                        "type", "notification_match",
                        "ruleId", rule.getId().toString(),
                        "listingId", listing.getId().toString(),
                        "description", listing.getItemDescription(),
                        "ruleName", rule.getNlRule()
                );
                messagingTemplate.convertAndSendToUser(
                        userId, "/queue/notifications", wsPayload);
                log.debug("WebSocket notification pushed to user {}", userId);
            } catch (Exception wsEx) {
                log.warn("WebSocket notification push failed (non-fatal): {}", wsEx.getMessage());
            }
        } catch (MailException e) {
            log.error("Failed to send notification email: rule={}, listing={}, to={}, error={}",
                    rule.getId(), listing.getId(), email, e.getMessage());
        }
    }

    private String buildSubject(Listing listing) {
        String intent = listing.getIntent() != null ? listing.getIntent().name().toUpperCase() : "LISTING";
        String desc = listing.getItemDescription();
        if (desc.length() > 60) {
            desc = desc.substring(0, 57) + "...";
        }
        return String.format("[Trade Intel] %s Alert: %s", intent, desc);
    }

    private String buildBody(NotificationRule rule, Listing listing) {
        StringBuilder sb = new StringBuilder();
        sb.append("A new listing matched your notification rule.\n\n");

        sb.append("YOUR RULE: ").append(rule.getNlRule()).append("\n\n");

        sb.append("LISTING DETAILS:\n");
        sb.append("  Description: ").append(listing.getItemDescription()).append("\n");
        sb.append("  Intent: ").append(listing.getIntent() != null ? listing.getIntent().name() : "unknown").append("\n");

        if (listing.getPrice() != null) {
            sb.append("  Price: ").append(listing.getPrice())
                    .append(" ").append(listing.getPriceCurrency() != null ? listing.getPriceCurrency() : "USD")
                    .append("\n");
        }
        if (listing.getPartNumber() != null && !listing.getPartNumber().isBlank()) {
            sb.append("  Part Number: ").append(listing.getPartNumber()).append("\n");
        }
        if (listing.getSenderName() != null && !listing.getSenderName().isBlank()) {
            sb.append("  Seller: ").append(listing.getSenderName()).append("\n");
        }
        if (listing.getConfidenceScore() != null) {
            sb.append("  Confidence: ").append(String.format("%.0f%%", listing.getConfidenceScore() * 100)).append("\n");
        }

        sb.append("\n--\nTrade Intelligence Platform\n");
        return sb.toString();
    }
}
