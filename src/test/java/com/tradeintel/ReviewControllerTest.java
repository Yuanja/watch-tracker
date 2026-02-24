package com.tradeintel;

import com.tradeintel.admin.AuditLogRepository;
import com.tradeintel.admin.ChatMessageRepository;
import com.tradeintel.admin.ChatSessionRepository;
import com.tradeintel.admin.UsageLedgerRepository;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.Category;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.ReviewQueueItem;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.notification.NotificationRuleRepository;
import com.tradeintel.processing.ReviewQueueItemRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.review.ReviewController}.
 *
 * <p>Tests verify the full review queue workflow: listing pending items,
 * resolving items with corrections, skipping items, and role-based access
 * enforcement (admin-only).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReviewControllerTest {

    private static final Logger log = LogManager.getLogger(ReviewControllerTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private WhatsappGroupRepository groupRepository;
    @Autowired private RawMessageRepository rawMessageRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private ReviewQueueItemRepository reviewQueueItemRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatSessionRepository chatSessionRepository;
    @Autowired private UsageLedgerRepository usageLedgerRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private TestDatabaseCleaner dbCleaner;

    private User regularUser;
    private User adminUser;
    private User uberAdmin;
    private ReviewQueueItem seededReviewItem;
    private Listing seededListing;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        // Users
        regularUser = TestHelper.createUser(userRepository, "user@review-test.com", UserRole.user);
        adminUser = TestHelper.createUser(userRepository, "admin@review-test.com", UserRole.admin);
        uberAdmin = TestHelper.createUser(userRepository, "uber@review-test.com", UserRole.uber_admin);

        // Group
        WhatsappGroup group = new WhatsappGroup();
        group.setWhapiGroupId("review-test-group@g.us");
        group.setGroupName("Review Test Group");
        group.setIsActive(true);
        group = groupRepository.save(group);

        // Raw message
        RawMessage rawMsg = new RawMessage();
        rawMsg.setGroup(group);
        rawMsg.setWhapiMsgId("review-test-msg-001");
        rawMsg.setSenderPhone("15550001234@s.whatsapp.net");
        rawMsg.setSenderName("Test Sender");
        rawMsg.setMessageBody("Maybe selling some Parker valves NOS");
        rawMsg.setMessageType("text");
        rawMsg.setIsForwarded(false);
        rawMsg.setTimestampWa(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        rawMsg.setProcessed(true);
        rawMsg = rawMessageRepository.save(rawMsg);

        // Pending review listing
        Listing listing = new Listing();
        listing.setRawMessage(rawMsg);
        listing.setGroup(group);
        listing.setIntent(IntentType.sell);
        listing.setConfidenceScore(0.65);
        listing.setItemDescription("Parker valves (uncertain)");
        listing.setOriginalText("Maybe selling some Parker valves NOS");
        listing.setSenderName("Test Sender");
        listing.setStatus(ListingStatus.pending_review);
        listing.setNeedsHumanReview(true);
        listing.setExpiresAt(OffsetDateTime.now().plusDays(60));
        seededListing = listingRepository.save(listing);

        // Review queue item
        ReviewQueueItem item = new ReviewQueueItem();
        item.setListing(seededListing);
        item.setRawMessage(rawMsg);
        item.setReason("Low confidence extraction (score: 0.65)");
        item.setLlmExplanation("Extraction confidence 0.65 is below auto-accept threshold");
        item.setSuggestedValues("{\"intent\":\"sell\",\"items\":[{\"description\":\"Parker valves\"}]}");
        item.setStatus("pending");
        seededReviewItem = reviewQueueItemRepository.save(item);

        log.info("ReviewControllerTest setUp: reviewItemId={}, listingId={}",
                seededReviewItem.getId(), seededListing.getId());
    }

    @AfterEach
    void tearDown() {
        dbCleaner.cleanAll();
    }

    // =========================================================================
    // GET /api/review — list pending
    // =========================================================================

    @Test
    @DisplayName("GET /api/review returns 200 with pending items when admin requests")
    void listPending_asAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(get("/api/review")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].id",
                        equalTo(seededReviewItem.getId().toString())))
                .andExpect(jsonPath("$.content[0].status", equalTo("pending")))
                .andExpect(jsonPath("$.content[0].reason", notNullValue()))
                .andExpect(jsonPath("$.content[0].originalMessageBody",
                        equalTo("Maybe selling some Parker valves NOS")))
                .andExpect(jsonPath("$.content[0].senderName", equalTo("Test Sender")));

        log.info("Verified GET /api/review returns pending items for admin");
    }

    @Test
    @DisplayName("GET /api/review returns 200 for uber_admin as well")
    void listPending_asUberAdmin_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);

        mockMvc.perform(get("/api/review")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));

        log.info("Verified GET /api/review returns 200 for uber_admin");
    }

    @Test
    @DisplayName("GET /api/review returns 403 for regular users")
    void listPending_asRegularUser_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(get("/api/review")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        log.info("Verified regular user cannot access review queue (403)");
    }

    @Test
    @DisplayName("GET /api/review redirects unauthenticated requests")
    void listPending_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/api/review")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /api/review supports pagination")
    void listPending_pagination_works() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Request page 0 with size 10
        mockMvc.perform(get("/api/review")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.size", equalTo(10)))
                .andExpect(jsonPath("$.number", equalTo(0)));

        // Request page 1 (should be empty)
        mockMvc.perform(get("/api/review")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        log.info("Verified pagination on GET /api/review");
    }

    // =========================================================================
    // POST /api/review/{id}/resolve — resolve with corrections
    // =========================================================================

    @Test
    @DisplayName("POST /api/review/{id}/resolve updates listing and resolves review item")
    void resolve_asAdmin_updatesListingAndResolvesItem() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Seed a category for the correction
        Category valves = new Category();
        valves.setName("Valves");
        valves.setIsActive(true);
        categoryRepository.save(valves);

        String resolveBody = """
                {
                  "itemDescription": "Parker ball valves NOS (corrected)",
                  "categoryName": "Valves",
                  "partNumber": "PK-BV-100",
                  "quantity": 10,
                  "price": 55.0,
                  "intent": "sell"
                }
                """;

        mockMvc.perform(post("/api/review/{id}/resolve", seededReviewItem.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(seededReviewItem.getId().toString())))
                .andExpect(jsonPath("$.status", equalTo("resolved")));

        // Verify the listing was updated.
        // Note: we check scalar/FK-id fields to avoid LazyInitializationException
        // since this test runs outside a persistent transaction.
        Listing updated = listingRepository.findById(seededListing.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ListingStatus.active);
        assertThat(updated.getNeedsHumanReview()).isFalse();
        assertThat(updated.getItemDescription()).isEqualTo("Parker ball valves NOS (corrected)");
        assertThat(updated.getPartNumber()).isEqualTo("PK-BV-100");
        assertThat(updated.getReviewedAt()).isNotNull();

        // Verify category was resolved by checking the listing still has a category FK set.
        // The Valves category was the only one seeded, so if itemCategory is non-null it must be it.
        assertThat(updated.getItemCategory()).isNotNull();

        // Verify review item is resolved
        ReviewQueueItem resolvedItem = reviewQueueItemRepository
                .findById(seededReviewItem.getId()).orElseThrow();
        assertThat(resolvedItem.getStatus()).isEqualTo("resolved");
        assertThat(resolvedItem.getResolvedAt()).isNotNull();

        log.info("Verified resolve updates listing and review item");
    }

    @Test
    @DisplayName("POST /api/review/{id}/resolve returns 403 for regular users")
    void resolve_asRegularUser_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(post("/api/review/{id}/resolve", seededReviewItem.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        log.info("Verified regular user cannot resolve reviews (403)");
    }

    @Test
    @DisplayName("POST /api/review/{id}/resolve returns 404 for nonexistent review item")
    void resolve_nonexistentId_returns404() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/review/{id}/resolve",
                        "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        log.info("Verified 404 for nonexistent review item on resolve");
    }

    @Test
    @DisplayName("POST /api/review/{id}/resolve returns 400 when item already resolved")
    void resolve_alreadyResolved_returns400() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Resolve the item first
        mockMvc.perform(post("/api/review/{id}/resolve", seededReviewItem.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Try to resolve again
        mockMvc.perform(post("/api/review/{id}/resolve", seededReviewItem.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        log.info("Verified 400 when resolving already-resolved item");
    }

    // =========================================================================
    // POST /api/review/{id}/skip — skip review
    // =========================================================================

    @Test
    @DisplayName("POST /api/review/{id}/skip marks review as skipped without modifying listing")
    void skip_asAdmin_marksSkipped() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/review/{id}/skip", seededReviewItem.getId())
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(seededReviewItem.getId().toString())))
                .andExpect(jsonPath("$.status", equalTo("skipped")));

        // Verify listing was NOT modified
        Listing listing = listingRepository.findById(seededListing.getId()).orElseThrow();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.pending_review);
        assertThat(listing.getNeedsHumanReview()).isTrue();

        // Verify review item is skipped
        ReviewQueueItem skipped = reviewQueueItemRepository
                .findById(seededReviewItem.getId()).orElseThrow();
        assertThat(skipped.getStatus()).isEqualTo("skipped");
        assertThat(skipped.getResolvedBy()).isNotNull();
        assertThat(skipped.getResolvedAt()).isNotNull();

        log.info("Verified skip leaves listing unchanged and marks review as skipped");
    }

    @Test
    @DisplayName("POST /api/review/{id}/skip returns 403 for regular users")
    void skip_asRegularUser_returns403() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(post("/api/review/{id}/skip", seededReviewItem.getId())
                        .header("Authorization", auth))
                .andExpect(status().isForbidden());

        log.info("Verified regular user cannot skip reviews (403)");
    }

    @Test
    @DisplayName("POST /api/review/{id}/skip returns 404 for nonexistent review item")
    void skip_nonexistentId_returns404() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        mockMvc.perform(post("/api/review/{id}/skip",
                        "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());

        log.info("Verified 404 for nonexistent review item on skip");
    }

    @Test
    @DisplayName("POST /api/review/{id}/skip returns 400 when item already skipped")
    void skip_alreadySkipped_returns400() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Skip once
        mockMvc.perform(post("/api/review/{id}/skip", seededReviewItem.getId())
                        .header("Authorization", auth))
                .andExpect(status().isOk());

        // Try to skip again
        mockMvc.perform(post("/api/review/{id}/skip", seededReviewItem.getId())
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest());

        log.info("Verified 400 when skipping already-skipped item");
    }

    // =========================================================================
    // Review queue is empty after processing
    // =========================================================================

    @Test
    @DisplayName("GET /api/review returns empty page after all items resolved")
    void listPending_afterAllResolved_returnsEmpty() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, adminUser);

        // Resolve the seeded item
        mockMvc.perform(post("/api/review/{id}/resolve", seededReviewItem.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // List pending should now be empty
        mockMvc.perform(get("/api/review")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)));

        log.info("Verified empty pending queue after resolution");
    }
}
