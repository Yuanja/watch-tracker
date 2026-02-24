package com.tradeintel;

import com.tradeintel.admin.AuditLogRepository;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.auth.JwtTokenProvider;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.listing.ListingRepository;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.listing.ListingController}.
 *
 * <p>Tests verify the full request/response cycle for listing search, stats retrieval,
 * admin update, uber_admin soft-delete, and role-based access enforcement, all backed
 * by the H2 in-memory database.</p>
 *
 * <p>Each test method starts with a clean DB state because we perform a full teardown
 * and re-seed in {@link #setUp()}. The {@link DirtiesContext} annotation at the
 * {@code AFTER_CLASS} level resets the Spring context between test classes.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListingControllerTest {

    private static final Logger log = LogManager.getLogger(ListingControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WhatsappGroupRepository groupRepository;

    @Autowired
    private RawMessageRepository rawMessageRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TestDatabaseCleaner dbCleaner;

    /** Regular authenticated user — read access, no mutation rights. */
    private User regularUser;

    /** Admin user — may update listings. */
    private User adminUser;

    /** Uber-admin — may soft-delete listings. */
    private User uberAdmin;

    /** The listing seeded in {@link #setUp()} for use across tests. */
    private Listing seededListing;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        regularUser = TestHelper.createUser(userRepository, "user@listings-test.com",       UserRole.user);
        adminUser   = TestHelper.createUser(userRepository, "admin@listings-test.com",      UserRole.admin);
        uberAdmin   = TestHelper.createUser(userRepository, "uber@listings-test.com",       UserRole.uber_admin);

        // Seed one WhatsApp group and one raw message as listing prerequisites.
        WhatsappGroup group = new WhatsappGroup();
        group.setWhapiGroupId("listing-test-group@g.us");
        group.setGroupName("Listing Test Group");
        group.setIsActive(true);
        group = groupRepository.save(group);

        RawMessage rawMsg = new RawMessage();
        rawMsg.setGroup(group);
        rawMsg.setWhapiMsgId("listing-test-msg-001");
        rawMsg.setSenderPhone("15550001234@s.whatsapp.net");
        rawMsg.setSenderName("Test Seller");
        rawMsg.setMessageBody("Selling 10x Parker valves NOS $50 each");
        rawMsg.setMessageType("text");
        rawMsg.setIsForwarded(false);
        rawMsg.setTimestampWa(OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC));
        rawMsg.setProcessed(true);
        rawMsg = rawMessageRepository.save(rawMsg);

        // Seed one active listing.
        Listing listing = new Listing();
        listing.setRawMessage(rawMsg);
        listing.setGroup(group);
        listing.setIntent(IntentType.sell);
        listing.setConfidenceScore(0.95);
        listing.setItemDescription("Parker valves NOS");
        listing.setOriginalText("Selling 10x Parker valves NOS $50 each");
        listing.setSenderName("Test Seller");
        listing.setSenderPhone("15550001234@s.whatsapp.net");
        listing.setStatus(ListingStatus.active);
        listing.setNeedsHumanReview(false);
        seededListing = listingRepository.save(listing);

        log.info("ListingControllerTest setUp complete: listingId={}", seededListing.getId());
    }

    /**
     * Tears down all data seeded by this test class in FK-safe order so that
     * subsequent test classes that share the named H2 database
     * ({@code jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1}) are not affected by
     * leftover listing rows which reference raw_messages.
     */
    @AfterEach
    void tearDown() {
        dbCleaner.cleanAll();
    }

    // =========================================================================
    // GET /api/listings — search/filter
    // =========================================================================

    @Test
    @DisplayName("GET /api/listings returns 200 with paginated listings when authenticated user requests")
    void listListings_authenticated_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(get("/api/listings")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content",       notNullValue()))
                .andExpect(jsonPath("$.totalElements", equalTo(1)))
                .andExpect(jsonPath("$.content[0].itemDescription", equalTo("Parker valves NOS")));

        log.info("Verified GET /api/listings returns 200 with active listing");
    }

    @Test
    @DisplayName("GET /api/listings redirects unauthenticated requests (OAuth2)")
    void listListings_unauthenticated_redirectsToOAuth2() throws Exception {
        mockMvc.perform(get("/api/listings")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /api/listings filters correctly by intent=want (returns empty when only sell exists)")
    void listListings_filterByWantIntent_returnsEmpty() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(get("/api/listings")
                        .param("intent", "want")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)));
    }

    // =========================================================================
    // GET /api/listings/stats — aggregated counts
    // =========================================================================

    @Test
    @DisplayName("GET /api/listings/stats returns 200 with aggregated counts for authenticated user")
    void getListingStats_authenticated_returns200() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(get("/api/listings/stats")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total",    notNullValue()))
                .andExpect(jsonPath("$.byIntent", notNullValue()))
                .andExpect(jsonPath("$.byStatus", notNullValue()));

        log.info("Verified GET /api/listings/stats returns aggregated statistics");
    }

    @Test
    @DisplayName("GET /api/listings/stats reflects correct counts for the seeded listing")
    void getListingStats_reflectsSeededData() throws Exception {
        String auth = TestHelper.bearerHeader(jwtTokenProvider, regularUser);

        mockMvc.perform(get("/api/listings/stats")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // The seeded listing is active and has intent=sell.
                .andExpect(jsonPath("$.total",             equalTo(1)))
                .andExpect(jsonPath("$.byIntent.sell",     equalTo(1)))
                .andExpect(jsonPath("$.byIntent.want",     equalTo(0)))
                .andExpect(jsonPath("$.byStatus.active",   equalTo(1)));
    }

    // =========================================================================
    // PUT /api/listings/{id} — admin update
    // =========================================================================

    @Test
    @DisplayName("PUT /api/listings/{id} returns 200 and updates listing fields when admin calls")
    void updateListing_asAdmin_returns200() throws Exception {
        String auth      = TestHelper.bearerHeader(jwtTokenProvider, adminUser);
        String listingId = seededListing.getId().toString();

        mockMvc.perform(put("/api/listings/{id}", listingId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemDescription": "Parker 10 valves NOS — updated by admin",
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",              equalTo(listingId)))
                .andExpect(jsonPath("$.itemDescription", equalTo("Parker 10 valves NOS — updated by admin")));

        log.info("Verified PUT /api/listings/{} returns 200 for admin", listingId);
    }

    @Test
    @DisplayName("PUT /api/listings/{id} returns 403 when regular user attempts to update")
    void updateListing_asRegularUser_returns403() throws Exception {
        String auth      = TestHelper.bearerHeader(jwtTokenProvider, regularUser);
        String listingId = seededListing.getId().toString();

        mockMvc.perform(put("/api/listings/{id}", listingId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemDescription": "Unauthorized edit attempt"}
                                """))
                .andExpect(status().isForbidden());

        log.info("Verified regular user cannot update listings (403)");
    }

    // =========================================================================
    // DELETE /api/listings/{id} — uber_admin soft-delete
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/listings/{id} returns 204 when uber_admin soft-deletes a listing")
    void softDeleteListing_asUberAdmin_returns204() throws Exception {
        String auth      = TestHelper.bearerHeader(jwtTokenProvider, uberAdmin);
        String listingId = seededListing.getId().toString();

        mockMvc.perform(delete("/api/listings/{id}", listingId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // After soft-delete the listing is excluded from the default search results.
        mockMvc.perform(get("/api/listings")
                        .header("Authorization", auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)));

        log.info("Verified DELETE /api/listings/{} soft-deletes for uber_admin", listingId);
    }

    @Test
    @DisplayName("DELETE /api/listings/{id} returns 403 when regular user attempts to delete")
    void softDeleteListing_asRegularUser_returns403() throws Exception {
        String auth      = TestHelper.bearerHeader(jwtTokenProvider, regularUser);
        String listingId = seededListing.getId().toString();

        mockMvc.perform(delete("/api/listings/{id}", listingId)
                        .header("Authorization", auth))
                .andExpect(status().isForbidden());

        log.info("Verified regular user cannot delete listings (403)");
    }

    @Test
    @DisplayName("DELETE /api/listings/{id} returns 403 when admin (not uber_admin) attempts to delete")
    void softDeleteListing_asAdmin_returns403() throws Exception {
        String auth      = TestHelper.bearerHeader(jwtTokenProvider, adminUser);
        String listingId = seededListing.getId().toString();

        mockMvc.perform(delete("/api/listings/{id}", listingId)
                        .header("Authorization", auth))
                .andExpect(status().isForbidden());

        log.info("Verified admin (non-uber) cannot delete listings (403)");
    }
}
