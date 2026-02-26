package com.tradeintel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeintel.admin.ChatMessageRepository;
import com.tradeintel.admin.ChatSessionRepository;
import com.tradeintel.admin.UsageLedgerRepository;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.ChatSession;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.entity.UsageLedger;
import com.tradeintel.common.openai.OpenAIClient;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.chat.ChatController}.
 *
 * <p>The OpenAI client is mocked to avoid real API calls during testing. Tests
 * verify session CRUD, message processing, cost tracking, and authentication
 * enforcement.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChatControllerTest {

    private static final Logger log = LogManager.getLogger(ChatControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UsageLedgerRepository usageLedgerRepository;

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

        testUser = TestHelper.createUser(userRepository, "chat-user@example.com", UserRole.user);

        // Default mock: return a simple text response from OpenAI
        when(openAIClient.chatCompletion(anyString(), anyList(), anyDouble()))
                .thenReturn(new OpenAIClient.ChatCompletionResponse(
                        "Hello! I can help you search for listings. What are you looking for?",
                        "gpt-4o",
                        150,
                        25
                ));

        log.info("ChatControllerTest setUp complete: user={}", testUser.getId());
    }

    // -------------------------------------------------------------------------
    // POST /api/chat/sessions — create session
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/chat/sessions creates a new session with a title")
    void createSession_withTitle_returnsCreatedSession() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(post("/api/chat/sessions")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"My Test Chat\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("My Test Chat")));

        log.info("Verified POST /api/chat/sessions creates session with title");
    }

    @Test
    @DisplayName("POST /api/chat/sessions creates a session with default title when no body")
    void createSession_noBody_returnsSessionWithDefaultTitle() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(post("/api/chat/sessions")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("New Chat")));
    }

    @Test
    @DisplayName("POST /api/chat/sessions returns 401 for unauthenticated requests")
    void createSession_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /api/chat/sessions — list sessions
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/chat/sessions returns paginated sessions for the authenticated user")
    void listSessions_withSessions_returnsPaginatedList() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        // Create two sessions
        createSessionForUser(testUser, "Chat 1");
        createSessionForUser(testUser, "Chat 2");

        mockMvc.perform(get("/api/chat/sessions")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", equalTo(2)));

        log.info("Verified GET /api/chat/sessions returns paginated sessions");
    }

    @Test
    @DisplayName("GET /api/chat/sessions does not return other users' sessions")
    void listSessions_otherUsersSessions_areNotReturned() throws Exception {
        User otherUser = TestHelper.createUser(userRepository, "other@example.com", UserRole.user);
        createSessionForUser(otherUser, "Other User Chat");
        createSessionForUser(testUser, "My Chat");

        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/chat/sessions")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", equalTo("My Chat")));
    }

    // -------------------------------------------------------------------------
    // GET /api/chat/sessions/{id} — session detail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/chat/sessions/{id} returns session with messages")
    void getSessionDetail_ownSession_returnsWithMessages() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        ChatSession session = createSessionForUser(testUser, "Detail Test");
        addMessageToSession(session, "user", "Hello");
        addMessageToSession(session, "assistant", "Hi there!");

        mockMvc.perform(get("/api/chat/sessions/{id}", session.getId())
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(session.getId().toString())))
                .andExpect(jsonPath("$.title", equalTo("Detail Test")))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role", equalTo("user")))
                .andExpect(jsonPath("$.messages[1].role", equalTo("assistant")));

        log.info("Verified GET /api/chat/sessions/{id} returns session detail with messages");
    }

    @Test
    @DisplayName("GET /api/chat/sessions/{id} returns 404 for non-existent session")
    void getSessionDetail_nonExistent_returns404() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/chat/sessions/{id}", UUID.randomUUID())
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/chat/sessions/{id} returns 404 for another user's session")
    void getSessionDetail_otherUserSession_returns404() throws Exception {
        User otherUser = TestHelper.createUser(userRepository, "other2@example.com", UserRole.user);
        ChatSession otherSession = createSessionForUser(otherUser, "Other Session");

        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/chat/sessions/{id}", otherSession.getId())
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/chat/sessions/{id}/messages — send message
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/chat/sessions/{id}/messages sends message and receives AI response")
    void sendMessage_validSession_returnsAIResponse() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        ChatSession session = createSessionForUser(testUser, "AI Chat");

        mockMvc.perform(post("/api/chat/sessions/{id}/messages", session.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"What Parker valves are available?\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.message.role", equalTo("assistant")))
                .andExpect(jsonPath("$.message.content", notNullValue()))
                .andExpect(jsonPath("$.message.modelUsed", equalTo("gpt-4o")))
                .andExpect(jsonPath("$.message.inputTokens", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.message.outputTokens", greaterThanOrEqualTo(1)));

        // Verify messages were persisted
        var messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        assertEquals(2, messages.size(), "Should have user + assistant messages");
        assertEquals("user", messages.get(0).getRole());
        assertEquals("assistant", messages.get(1).getRole());

        log.info("Verified POST /api/chat/sessions/{id}/messages produces AI response");
    }

    @Test
    @DisplayName("POST /api/chat/sessions/{id}/messages with tool call triggers tool execution")
    void sendMessage_withToolCall_executesToolAndReturnsResponse() throws Exception {
        // First call returns a tool call, second call returns final response
        when(openAIClient.chatCompletion(anyString(), anyList(), anyDouble()))
                .thenReturn(new OpenAIClient.ChatCompletionResponse(
                        "Let me search for that. {\"tool\": \"search_listings\", \"params\": {\"query\": \"Parker valves\"}}",
                        "gpt-4o", 200, 50
                ))
                .thenReturn(new OpenAIClient.ChatCompletionResponse(
                        "I found some Parker valve listings for you. Currently there are no active listings matching your query.",
                        "gpt-4o", 300, 60
                ));

        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        ChatSession session = createSessionForUser(testUser, "Tool Call Chat");

        mockMvc.perform(post("/api/chat/sessions/{id}/messages", session.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Search for Parker valves\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message.role", equalTo("assistant")))
                .andExpect(jsonPath("$.message.content", notNullValue()))
                .andExpect(jsonPath("$.toolResults", hasSize(1)));

        log.info("Verified tool call detection and execution");
    }

    @Test
    @DisplayName("POST /api/chat/sessions/{id}/messages returns 404 for non-existent session")
    void sendMessage_nonExistentSession_returns404() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(post("/api/chat/sessions/{id}/messages", UUID.randomUUID())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Hello\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/chat/sessions/{id}/messages returns 400 when message is blank")
    void sendMessage_blankMessage_returns400() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        ChatSession session = createSessionForUser(testUser, "Blank Test");

        mockMvc.perform(post("/api/chat/sessions/{id}/messages", session.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/chat/cost — cost summary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/chat/cost returns cost summary for the authenticated user")
    void getCostSummary_withUsage_returnsSummary() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        // Seed a usage ledger entry
        UsageLedger ledger = new UsageLedger();
        ledger.setUser(testUser);
        ledger.setPeriodDate(LocalDate.now());
        ledger.setTotalInputTokens(1000L);
        ledger.setTotalOutputTokens(500L);
        ledger.setTotalCostUsd(BigDecimal.valueOf(0.05));
        ledger.setSessionCount(3);
        usageLedgerRepository.save(ledger);

        mockMvc.perform(get("/api/chat/cost")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInputTokens", equalTo(1000)))
                .andExpect(jsonPath("$.totalOutputTokens", equalTo(500)))
                .andExpect(jsonPath("$.sessionCount", equalTo(3)))
                .andExpect(jsonPath("$.dailyBreakdown", hasSize(1)))
                .andExpect(jsonPath("$.dailyBreakdown[0].inputTokens", equalTo(1000)));

        log.info("Verified GET /api/chat/cost returns cost summary");
    }

    @Test
    @DisplayName("GET /api/chat/cost returns zeros when no usage data exists")
    void getCostSummary_noUsage_returnsZeros() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);

        mockMvc.perform(get("/api/chat/cost")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInputTokens", equalTo(0)))
                .andExpect(jsonPath("$.totalOutputTokens", equalTo(0)))
                .andExpect(jsonPath("$.sessionCount", equalTo(0)))
                .andExpect(jsonPath("$.dailyBreakdown", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Cost tracking verification
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sending a message tracks cost in the usage ledger")
    void sendMessage_tracksCostInUsageLedger() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, testUser);
        ChatSession session = createSessionForUser(testUser, "Cost Track Test");

        mockMvc.perform(post("/api/chat/sessions/{id}/messages", session.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Hello AI\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify cost was tracked
        List<UsageLedger> entries = usageLedgerRepository.findByUserIdAndDateRange(
                testUser.getId(), LocalDate.now(), LocalDate.now());

        assertEquals(1, entries.size(), "Should have one usage ledger entry");
        UsageLedger entry = entries.get(0);
        assertEquals(150L, entry.getTotalInputTokens(), "Input tokens should match mock response");
        assertEquals(25L, entry.getTotalOutputTokens(), "Output tokens should match mock response");

        log.info("Verified cost tracking after sending message");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ChatSession createSessionForUser(User user, String title) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle(title);
        return sessionRepository.save(session);
    }

    private void addMessageToSession(ChatSession session, String role, String content) {
        com.tradeintel.common.entity.ChatMessage msg = new com.tradeintel.common.entity.ChatMessage();
        msg.setSession(session);
        msg.setRole(role);
        msg.setContent(content);
        msg.setInputTokens(0);
        msg.setOutputTokens(0);
        msg.setCostUsd(BigDecimal.ZERO);
        messageRepository.save(msg);
    }
}
