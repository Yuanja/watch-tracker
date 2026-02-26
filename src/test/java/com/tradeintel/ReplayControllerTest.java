package com.tradeintel;

import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.entity.WhatsappGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Note on authentication behaviour: This application uses Spring Security OAuth2 login.
// When an unauthenticated request arrives at a protected endpoint, Spring Security redirects
// to the OAuth2 authorization endpoint (HTTP 302) rather than returning 401.
// Tests that verify "unauthenticated requests are rejected" therefore assert 302 — the
// redirect itself is the enforcement mechanism; the client never reaches the controller.

/**
 * End-to-end integration tests for {@link com.tradeintel.replay.ReplayController}.
 *
 * <p>Tests verify the full request/response cycle for group listing, paginated
 * message retrieval, message search, and authentication enforcement, all backed
 * by the H2 in-memory database.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReplayControllerTest {

    private static final Logger log = LogManager.getLogger(ReplayControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WhatsappGroupRepository groupRepository;

    @Autowired
    private RawMessageRepository rawMessageRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TestDatabaseCleaner dbCleaner;

    /** The authenticated user used in all test requests that require auth. */
    private User testUser;

    /** Active group seeded before each test. */
    private WhatsappGroup activeGroup;

    /** Inactive group — should be excluded from the groups listing. */
    private WhatsappGroup inactiveGroup;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        testUser = TestHelper.createUser(userRepository, "replay-user@example.com", UserRole.user);

        activeGroup = createGroup("group-active@g.us", "Active Trade Group", true);
        inactiveGroup = createGroup("group-inactive@g.us", "Inactive Group", false);

        // Seed two messages in the active group.
        seedMessage(activeGroup, "msg-replay-001", "Alice", "Selling 10x Parker valves", 1700000001L);
        seedMessage(activeGroup, "msg-replay-002", "Bob",   "Buying 5x pumps NOS",       1700000002L);

        log.info("ReplayControllerTest setUp complete: 2 messages in active group");
    }

    // -------------------------------------------------------------------------
    // GET /api/messages/groups
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/messages/groups returns only active groups when authenticated")
    void listGroups_authenticated_returnsActiveGroupsOnly() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/messages/groups")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].whapiGroupId", equalTo("group-active@g.us")))
                .andExpect(jsonPath("$[0].groupName",    equalTo("Active Trade Group")))
                .andExpect(jsonPath("$[0].isActive",     equalTo(true)));

        log.info("Verified /api/messages/groups returns only active groups");
    }

    @Test
    @DisplayName("GET /api/messages/groups returns 401 for unauthenticated requests")
    void listGroups_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/messages/groups")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/messages/groups/{groupId}/messages
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/messages/groups/{id}/messages returns paginated messages for a known group")
    void getGroupMessages_knownGroup_returnsPagedMessages() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String groupId = activeGroup.getId().toString();

        mockMvc.perform(get("/api/messages/groups/{groupId}/messages", groupId)
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content",        notNullValue()))
                .andExpect(jsonPath("$.content",        hasSize(2)))
                .andExpect(jsonPath("$.totalElements",  equalTo(2)))
                .andExpect(jsonPath("$.content[0].groupName", equalTo("Active Trade Group")));

        log.info("Verified paginated messages returned for groupId={}", groupId);
    }

    @Test
    @DisplayName("GET /api/messages/groups/{id}/messages returns 404 for an unknown group UUID")
    void getGroupMessages_unknownGroup_returns404() throws Exception {
        String auth          = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String unknownGroupId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/messages/groups/{groupId}/messages", unknownGroupId)
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/messages/groups/{id}/messages returns 401 for unauthenticated requests")
    void getGroupMessages_unauthenticated_returns401() throws Exception {
        String groupId = activeGroup.getId().toString();

        mockMvc.perform(get("/api/messages/groups/{groupId}/messages", groupId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/messages/groups/{id}/messages returns empty page for group with no messages")
    void getGroupMessages_groupWithNoMessages_returnsEmptyPage() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        WhatsappGroup emptyGroup = createGroup("group-empty@g.us", "Empty Group", true);

        mockMvc.perform(get("/api/messages/groups/{groupId}/messages", emptyGroup.getId())
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content",       hasSize(0)))
                .andExpect(jsonPath("$.totalElements", equalTo(0)));
    }

    // -------------------------------------------------------------------------
    // GET /api/messages — all messages (paginated)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/messages returns paginated messages across all groups")
    void getMessages_authenticated_returnsAllMessages() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/messages")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content",       notNullValue()))
                .andExpect(jsonPath("$.totalElements", equalTo(2)));
    }

    @Test
    @DisplayName("GET /api/messages returns 401 for unauthenticated requests")
    void getMessages_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/messages")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/messages/search
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/messages/search returns messages for a given group without sender filter")
    void searchMessages_noSenderFilter_returnsAllGroupMessages() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String groupId = activeGroup.getId().toString();

        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", groupId)
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(2)));

        log.info("Verified search with no filters returns all group messages");
    }

    @Test
    @DisplayName("GET /api/messages/search filters by sender name")
    void searchMessages_withSenderFilter_returnsFilteredMessages() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String groupId = activeGroup.getId().toString();

        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", groupId)
                        .param("sender",  "Alice")
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].senderName", equalTo("Alice")));

        log.info("Verified search filtered by sender=Alice returns 1 result");
    }

    @Test
    @DisplayName("GET /api/messages/search performs cross-group search when groupId is omitted")
    void searchMessages_missingGroupId_returnsCrossGroupResults() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/messages/search")
                        .param("q",    "valves")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/messages/search returns 404 for an unknown groupId")
    void searchMessages_unknownGroupId_returns404() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String unknown = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", unknown)
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/messages/search filters by text query on message body")
    void searchMessages_withTextQuery_returnsMatchingMessages() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String groupId = activeGroup.getId().toString();

        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", groupId)
                        .param("q",       "valves")
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].senderName", equalTo("Alice")));

        log.info("Verified search filtered by q=valves returns 1 result");
    }

    @Test
    @DisplayName("GET /api/messages/search with text query returns no results for non-matching text")
    void searchMessages_withNonMatchingTextQuery_returnsEmpty() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String groupId = activeGroup.getId().toString();

        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", groupId)
                        .param("q",       "nonexistent_term_xyz")
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)));

        log.info("Verified search with non-matching q returns empty");
    }

    @Test
    @DisplayName("GET /api/messages/search combines text query and sender filter")
    void searchMessages_withTextQueryAndSender_returnsCombinedResults() throws Exception {
        String auth    = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        String groupId = activeGroup.getId().toString();

        // Search for "pumps" by Bob - should match
        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", groupId)
                        .param("q",       "pumps")
                        .param("sender",  "Bob")
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].senderName", equalTo("Bob")));

        // Search for "valves" by Bob - should not match (Alice sent valves)
        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", groupId)
                        .param("q",       "valves")
                        .param("sender",  "Bob")
                        .param("page",    "0")
                        .param("size",    "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)));

        log.info("Verified combined text query + sender filter");
    }

    @Test
    @DisplayName("GET /api/messages/search with semantic param returns 200 (cross-group semantic search)")
    void searchMessages_withSemanticParam_returnsCrossGroupResults() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/messages/search")
                        .param("semantic", "industrial valve parts")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        log.info("Verified semantic search without groupId returns 200 with cross-group results");
    }

    @Test
    @DisplayName("GET /api/messages/search returns 401 for unauthenticated requests")
    void searchMessages_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/messages/search")
                        .param("groupId", activeGroup.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private WhatsappGroup createGroup(String whapiGroupId, String name, boolean active) {
        WhatsappGroup g = new WhatsappGroup();
        g.setWhapiGroupId(whapiGroupId);
        g.setGroupName(name);
        g.setIsActive(active);
        return groupRepository.save(g);
    }

    private RawMessage seedMessage(WhatsappGroup group, String msgId, String senderName,
                                   String body, long epochSeconds) {
        RawMessage msg = new RawMessage();
        msg.setGroup(group);
        msg.setWhapiMsgId(msgId);
        msg.setSenderPhone("1555000000" + epochSeconds % 10 + "@s.whatsapp.net");
        msg.setSenderName(senderName);
        msg.setMessageBody(body);
        msg.setMessageType("text");
        msg.setIsForwarded(false);
        msg.setTimestampWa(OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC));
        msg.setProcessed(false);
        return rawMessageRepository.save(msg);
    }
}
