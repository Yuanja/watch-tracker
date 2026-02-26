package com.tradeintel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.NotificationRule;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.openai.OpenAIClient;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.notification.NotificationController}.
 *
 * <p>The OpenAI client is mocked to simulate NL rule parsing without real API calls.
 * Tests verify CRUD operations, ownership enforcement, and authentication.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationControllerTest {

    private static final Logger log = LogManager.getLogger(NotificationControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRuleRepository ruleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAIClient openAIClient;

    @Autowired
    private TestDatabaseCleaner dbCleaner;

    private User testUser;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        testUser = TestHelper.createUser(userRepository, "notif-user@example.com", UserRole.user);

        // Mock the NL rule parser LLM call
        when(openAIClient.chatCompletion(anyString(), anyList(), anyDouble()))
                .thenReturn(new OpenAIClient.ChatCompletionResponse(
                        "{\"intent\": \"sell\", \"keywords\": [\"Parker\", \"valves\"], "
                                + "\"category_names\": [], \"price_min\": null, \"price_max\": 500}",
                        "gpt-4o-mini",
                        100,
                        50
                ));

        log.info("NotificationControllerTest setUp complete: user={}", testUser.getId());
    }

    // -------------------------------------------------------------------------
    // POST /api/notifications — create rule
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/notifications creates a notification rule from NL text")
    void createRule_validInput_returnsCreatedRule() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlRule\": \"Notify me when someone sells Parker valves under $500\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nlRule", equalTo("Notify me when someone sells Parker valves under $500")))
                .andExpect(jsonPath("$.parsedIntent", equalTo("sell")))
                .andExpect(jsonPath("$.parsedKeywords", hasSize(2)))
                .andExpect(jsonPath("$.isActive", equalTo(true)))
                .andExpect(jsonPath("$.notifyEmail", equalTo("notif-user@example.com")));

        assertEquals(1, ruleRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()).size());
        log.info("Verified POST /api/notifications creates rule with parsed fields");
    }

    @Test
    @DisplayName("POST /api/notifications with custom email uses that email")
    void createRule_withCustomEmail_usesProvidedEmail() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlRule\": \"Alert me about pumps\", \"notifyEmail\": \"custom@example.com\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notifyEmail", equalTo("custom@example.com")));
    }

    @Test
    @DisplayName("POST /api/notifications returns 400 when nlRule is blank")
    void createRule_blankRule_returns400() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlRule\": \"\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/notifications returns 401 for unauthenticated requests")
    void createRule_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlRule\": \"test\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/notifications — list rules
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/notifications returns all rules for the authenticated user")
    void listRules_withRules_returnsList() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        createRuleForUser(testUser, "Rule 1");
        createRuleForUser(testUser, "Rule 2");

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        log.info("Verified GET /api/notifications returns user's rules");
    }

    @Test
    @DisplayName("GET /api/notifications does not return other users' rules")
    void listRules_otherUsersRules_areNotReturned() throws Exception {
        User otherUser = TestHelper.createUser(userRepository, "other-notif@example.com", UserRole.user);
        createRuleForUser(otherUser, "Other User Rule");
        createRuleForUser(testUser, "My Rule");

        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nlRule", equalTo("My Rule")));
    }

    @Test
    @DisplayName("GET /api/notifications returns empty list when no rules exist")
    void listRules_noRules_returnsEmptyList() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // PUT /api/notifications/{id} — update rule
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/notifications/{id} updates the isActive flag")
    void updateRule_toggleActive_updatesSuccessfully() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        NotificationRule rule = createRuleForUser(testUser, "Toggle Rule");

        mockMvc.perform(put("/api/notifications/{id}", rule.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": false}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", equalTo(false)));

        log.info("Verified PUT /api/notifications/{id} toggles isActive");
    }

    @Test
    @DisplayName("PUT /api/notifications/{id} re-parses when nlRule changes")
    void updateRule_changeNlRule_reParses() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        NotificationRule rule = createRuleForUser(testUser, "Original Rule");

        mockMvc.perform(put("/api/notifications/{id}", rule.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nlRule\": \"Updated: notify about pumps\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nlRule", equalTo("Updated: notify about pumps")));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id} returns 404 for another user's rule")
    void updateRule_otherUsersRule_returns404() throws Exception {
        User otherUser = TestHelper.createUser(userRepository, "other-update@example.com", UserRole.user);
        NotificationRule otherRule = createRuleForUser(otherUser, "Not mine");

        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(put("/api/notifications/{id}", otherRule.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": false}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/notifications/{id} returns 404 for non-existent rule")
    void updateRule_nonExistent_returns404() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(put("/api/notifications/{id}", UUID.randomUUID())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": false}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/notifications/{id} — delete rule
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/notifications/{id} deletes the rule")
    void deleteRule_ownRule_deletesSuccessfully() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        NotificationRule rule = createRuleForUser(testUser, "To Delete");

        mockMvc.perform(delete("/api/notifications/{id}", rule.getId())
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        assertTrue(ruleRepository.findById(rule.getId()).isEmpty());
        log.info("Verified DELETE /api/notifications/{id} removes rule");
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} returns 404 for another user's rule")
    void deleteRule_otherUsersRule_returns404() throws Exception {
        User otherUser = TestHelper.createUser(userRepository, "other-delete@example.com", UserRole.user);
        NotificationRule otherRule = createRuleForUser(otherUser, "Other Delete");

        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(delete("/api/notifications/{id}", otherRule.getId())
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} returns 404 for non-existent rule")
    void deleteRule_nonExistent_returns404() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(delete("/api/notifications/{id}", UUID.randomUUID())
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private NotificationRule createRuleForUser(User user, String nlRule) {
        NotificationRule rule = new NotificationRule();
        rule.setUser(user);
        rule.setNlRule(nlRule);
        rule.setNotifyChannel("email");
        rule.setNotifyEmail(user.getEmail());
        rule.setIsActive(true);
        return ruleRepository.save(rule);
    }
}
