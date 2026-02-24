package com.tradeintel.chat.tools;

import com.tradeintel.common.entity.User;
import com.tradeintel.notification.NotificationRuleService;
import com.tradeintel.notification.dto.NotificationRuleDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Chat tool that creates a notification rule for the current user.
 *
 * <p>Takes a natural language rule description and delegates to the
 * {@link NotificationRuleService} for parsing and persistence.</p>
 */
@Component
public class CreateNotificationTool {

    private static final Logger log = LogManager.getLogger(CreateNotificationTool.class);

    private final NotificationRuleService notificationRuleService;

    public CreateNotificationTool(NotificationRuleService notificationRuleService) {
        this.notificationRuleService = notificationRuleService;
    }

    /**
     * Executes the create_notification tool.
     *
     * @param params map containing "rule" (String) parameter
     * @param user   the current authenticated user
     * @return confirmation message string
     */
    public String execute(Map<String, Object> params, User user) {
        try {
            String ruleText = params.containsKey("rule") && params.get("rule") != null
                    ? params.get("rule").toString()
                    : null;

            if (ruleText == null || ruleText.isBlank()) {
                return "{\"error\": \"No rule text provided\"}";
            }

            NotificationRuleDTO created = notificationRuleService.createRule(
                    user, ruleText, user.getEmail());

            log.info("CreateNotificationTool: created rule id={} for user={}",
                    created.getId(), user.getId());

            return "{\"success\": true, \"ruleId\": \"" + created.getId()
                    + "\", \"message\": \"Notification rule created successfully. "
                    + "You will be notified at " + user.getEmail()
                    + " when matching listings appear.\"}";

        } catch (Exception e) {
            log.error("CreateNotificationTool failed", e);
            return "{\"error\": \"Failed to create notification: " + e.getMessage() + "\"}";
        }
    }
}
