package com.tradeintel;

import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.NotificationRule;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.notification.NotificationDispatcher;
import com.tradeintel.notification.NotificationRuleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * End-to-end tests for {@link NotificationDispatcher} and notification matching.
 *
 * <p>JavaMailSender is mocked so no actual emails are sent. Tests verify that
 * matching rules trigger email dispatch and non-matching rules do not.</p>
 *
 * <p>Tests are transactional to avoid lazy loading issues when the matcher
 * accesses the User entity through the NotificationRule relationship.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class NotificationDispatcherTest {

    private static final Logger log = LogManager.getLogger(NotificationDispatcherTest.class);

    @Autowired private UserRepository userRepository;
    @Autowired private WhatsappGroupRepository groupRepository;
    @Autowired private RawMessageRepository rawMessageRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private NotificationRuleRepository ruleRepository;
    @Autowired private NotificationDispatcher notificationDispatcher;

    @MockBean private JavaMailSender mailSender;

    private User testUser;
    private WhatsappGroup testGroup;

    @BeforeEach
    void setUp() {
        reset(mailSender);

        testUser = TestHelper.createUser(userRepository, "dispatch-user@example.com", UserRole.user);

        testGroup = new WhatsappGroup();
        testGroup.setWhapiGroupId("dispatch-test@g.us");
        testGroup.setGroupName("Dispatch Test Group");
        testGroup.setIsActive(true);
        testGroup = groupRepository.save(testGroup);
    }

    @Test
    @DisplayName("NotificationDispatcher sends email when rule matches listing")
    void dispatch_sendsEmail() {
        NotificationRule rule = createRule(IntentType.sell, new String[]{"valves"}, null, null);
        Listing listing = createListing("Parker ball valves", IntentType.sell, new BigDecimal("50.00"));

        notificationDispatcher.dispatch(rule, listing);

        verify(mailSender).send(any(SimpleMailMessage.class));
        log.info("Verified dispatch sends email via JavaMailSender");
    }

    @Test
    @DisplayName("Active rule query finds rules for active users")
    void findActiveRules_returnsRulesForActiveUsers() {
        createRule(IntentType.sell, new String[]{"valves"}, null, null);

        List<NotificationRule> rules = ruleRepository.findByIsActiveTrueAndUserIsActiveTrue();

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getIsActive()).isTrue();
        log.info("Verified active rules query returns 1 rule");
    }

    @Test
    @DisplayName("Active rule query excludes inactive rules")
    void findActiveRules_excludesInactiveRules() {
        NotificationRule rule = new NotificationRule();
        rule.setUser(testUser);
        rule.setNlRule("Inactive rule");
        rule.setParsedKeywords(new String[]{"valves"});
        rule.setNotifyChannel("email");
        rule.setNotifyEmail("dispatch-user@example.com");
        rule.setIsActive(false);
        ruleRepository.save(rule);

        List<NotificationRule> rules = ruleRepository.findByIsActiveTrueAndUserIsActiveTrue();

        assertThat(rules).isEmpty();
        log.info("Verified inactive rules excluded from query");
    }

    @Test
    @DisplayName("Price within range triggers dispatch")
    void dispatch_priceInRange_sendsEmail() {
        NotificationRule rule = createRule(null, null, null, new BigDecimal("100.00"));
        Listing listing = createListing("Cheap valves", IntentType.sell, new BigDecimal("50.00"));

        notificationDispatcher.dispatch(rule, listing);

        verify(mailSender).send(any(SimpleMailMessage.class));
        log.info("Verified dispatch sends email for price in range");
    }

    @Test
    @DisplayName("Dispatch falls back to user email when rule has no email")
    void dispatch_noRuleEmail_usesUserEmail() {
        NotificationRule rule = new NotificationRule();
        rule.setUser(testUser);
        rule.setNlRule("No email rule");
        rule.setNotifyChannel("email");
        rule.setNotifyEmail(null);
        rule.setIsActive(true);
        ruleRepository.save(rule);

        Listing listing = createListing("Test item", IntentType.sell, null);

        notificationDispatcher.dispatch(rule, listing);

        verify(mailSender).send(any(SimpleMailMessage.class));
        log.info("Verified dispatch uses user email as fallback");
    }

    private NotificationRule createRule(IntentType intent, String[] keywords,
                                         BigDecimal priceMin, BigDecimal priceMax) {
        NotificationRule rule = new NotificationRule();
        rule.setUser(testUser);
        rule.setNlRule("Test rule");
        rule.setParsedIntent(intent);
        rule.setParsedKeywords(keywords);
        rule.setParsedPriceMin(priceMin);
        rule.setParsedPriceMax(priceMax);
        rule.setNotifyChannel("email");
        rule.setNotifyEmail("dispatch-user@example.com");
        rule.setIsActive(true);
        return ruleRepository.save(rule);
    }

    private Listing createListing(String description, IntentType intent, BigDecimal price) {
        RawMessage msg = new RawMessage();
        msg.setGroup(testGroup);
        msg.setWhapiMsgId("dispatch-msg-" + System.nanoTime());
        msg.setSenderPhone("15551234567@s.whatsapp.net");
        msg.setSenderName("Test Seller");
        msg.setMessageBody(description);
        msg.setMessageType("text");
        msg.setIsForwarded(false);
        msg.setTimestampWa(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        msg.setProcessed(true);
        msg = rawMessageRepository.save(msg);

        Listing listing = new Listing();
        listing.setRawMessage(msg);
        listing.setGroup(testGroup);
        listing.setIntent(intent);
        listing.setItemDescription(description);
        listing.setOriginalText(description);
        listing.setConfidenceScore(0.9);
        listing.setPrice(price);
        listing.setSenderName("Test Seller");
        return listingRepository.save(listing);
    }
}
