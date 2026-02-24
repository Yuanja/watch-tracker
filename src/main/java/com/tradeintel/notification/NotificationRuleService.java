package com.tradeintel.notification;

import com.tradeintel.common.entity.Category;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.NotificationRule;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.notification.dto.NotificationRuleDTO;
import com.tradeintel.notification.dto.ParsedRule;
import com.tradeintel.notification.dto.UpdateRuleRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing notification rule CRUD operations and NL parsing.
 *
 * <p>Delegates rule parsing to {@link NLRuleParser} and resolves category
 * names to their database UUIDs via {@link CategoryRepository}.</p>
 */
@Service
public class NotificationRuleService {

    private static final Logger log = LogManager.getLogger(NotificationRuleService.class);

    private final NotificationRuleRepository ruleRepository;
    private final NLRuleParser nlRuleParser;
    private final CategoryRepository categoryRepository;

    public NotificationRuleService(NotificationRuleRepository ruleRepository,
                                   NLRuleParser nlRuleParser,
                                   CategoryRepository categoryRepository) {
        this.ruleRepository = ruleRepository;
        this.nlRuleParser = nlRuleParser;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Lists all notification rules belonging to the given user.
     *
     * @param userId the user's UUID
     * @return list of notification rule DTOs
     */
    @Transactional(readOnly = true)
    public List<NotificationRuleDTO> listUserRules(UUID userId) {
        return ruleRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationRuleDTO::fromEntity)
                .toList();
    }

    /**
     * Creates a new notification rule by parsing the NL description
     * and saving the structured result.
     *
     * @param user        the rule owner
     * @param nlRule      natural language rule description
     * @param notifyEmail optional email address (defaults to user's email)
     * @return the created rule as a DTO
     */
    @Transactional
    public NotificationRuleDTO createRule(User user, String nlRule, String notifyEmail) {
        log.info("Creating notification rule for user={}: '{}'", user.getId(), nlRule);

        ParsedRule parsed = nlRuleParser.parse(nlRule);

        NotificationRule rule = new NotificationRule();
        rule.setUser(user);
        rule.setNlRule(nlRule);
        rule.setNotifyEmail(notifyEmail != null ? notifyEmail : user.getEmail());
        rule.setNotifyChannel("email");
        rule.setIsActive(true);

        applyParsedFields(rule, parsed);

        NotificationRule saved = ruleRepository.save(rule);
        log.info("Created notification rule id={} for user={}", saved.getId(), user.getId());

        return NotificationRuleDTO.fromEntity(saved);
    }

    /**
     * Updates an existing notification rule. Re-parses if the NL rule text changed.
     *
     * @param ruleId  the rule UUID
     * @param userId  the requesting user's UUID (for ownership validation)
     * @param request the update request
     * @return the updated rule as a DTO
     */
    @Transactional
    public NotificationRuleDTO updateRule(UUID ruleId, UUID userId, UpdateRuleRequest request) {
        NotificationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationRule", ruleId));

        if (!rule.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("NotificationRule", ruleId);
        }

        boolean nlRuleChanged = false;

        if (request.getNlRule() != null && !request.getNlRule().isBlank()) {
            if (!request.getNlRule().equals(rule.getNlRule())) {
                rule.setNlRule(request.getNlRule());
                nlRuleChanged = true;
            }
        }

        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        if (request.getNotifyEmail() != null) {
            rule.setNotifyEmail(request.getNotifyEmail());
        }

        if (nlRuleChanged) {
            log.info("Re-parsing notification rule id={} with updated text", ruleId);
            ParsedRule parsed = nlRuleParser.parse(rule.getNlRule());
            applyParsedFields(rule, parsed);
        }

        NotificationRule saved = ruleRepository.save(rule);
        log.info("Updated notification rule id={}", saved.getId());

        return NotificationRuleDTO.fromEntity(saved);
    }

    /**
     * Deletes a notification rule after validating ownership.
     *
     * @param ruleId the rule UUID
     * @param userId the requesting user's UUID
     */
    @Transactional
    public void deleteRule(UUID ruleId, UUID userId) {
        NotificationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationRule", ruleId));

        if (!rule.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("NotificationRule", ruleId);
        }

        ruleRepository.delete(rule);
        log.info("Deleted notification rule id={} for user={}", ruleId, userId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Applies parsed fields from the NL rule parser result onto the entity.
     * Resolves category names to UUIDs via the category repository.
     */
    private void applyParsedFields(NotificationRule rule, ParsedRule parsed) {
        // Intent
        if (parsed.intent() != null) {
            try {
                rule.setParsedIntent(IntentType.valueOf(parsed.intent().toLowerCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown intent from parser: {}", parsed.intent());
                rule.setParsedIntent(null);
            }
        } else {
            rule.setParsedIntent(null);
        }

        // Keywords
        if (parsed.keywords() != null && !parsed.keywords().isEmpty()) {
            rule.setParsedKeywords(parsed.keywords().toArray(new String[0]));
        } else {
            rule.setParsedKeywords(null);
        }

        // Categories: resolve names to UUIDs
        if (parsed.categoryNames() != null && !parsed.categoryNames().isEmpty()) {
            List<UUID> categoryIds = new ArrayList<>();
            for (String name : parsed.categoryNames()) {
                Optional<Category> cat = categoryRepository.findByNameIgnoreCase(name);
                cat.ifPresent(c -> categoryIds.add(c.getId()));
            }
            if (!categoryIds.isEmpty()) {
                rule.setParsedCategoryIds(categoryIds.toArray(new UUID[0]));
            } else {
                rule.setParsedCategoryIds(null);
            }
        } else {
            rule.setParsedCategoryIds(null);
        }

        // Price bounds
        rule.setParsedPriceMin(parsed.priceMin() != null
                ? BigDecimal.valueOf(parsed.priceMin()) : null);
        rule.setParsedPriceMax(parsed.priceMax() != null
                ? BigDecimal.valueOf(parsed.priceMax()) : null);
    }
}
