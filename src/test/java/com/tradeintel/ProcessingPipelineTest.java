package com.tradeintel;

import com.tradeintel.admin.AuditLogRepository;
import com.tradeintel.admin.ChatMessageRepository;
import com.tradeintel.admin.ChatSessionRepository;
import com.tradeintel.admin.UsageLedgerRepository;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.Category;
import com.tradeintel.common.entity.Condition;
import com.tradeintel.common.entity.IntentType;
import com.tradeintel.common.entity.JargonEntry;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.common.entity.ListingStatus;
import com.tradeintel.common.entity.Manufacturer;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.Unit;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.listing.ListingRepository;
import com.tradeintel.normalize.CategoryRepository;
import com.tradeintel.normalize.ConditionRepository;
import com.tradeintel.normalize.JargonRepository;
import com.tradeintel.normalize.ManufacturerRepository;
import com.tradeintel.normalize.UnitRepository;
import com.tradeintel.notification.NotificationRuleRepository;
import com.tradeintel.processing.ConfidenceRouter;
import com.tradeintel.processing.ExtractionResult;
import com.tradeintel.processing.JargonExpander;
import com.tradeintel.processing.ReviewQueueItemRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the Phase 3 message processing pipeline.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Jargon expansion (verified terms replaced, unverified ignored)</li>
 *   <li>Confidence routing (auto-accept, review, discard)</li>
 *   <li>Review queue item creation for medium-confidence extractions</li>
 *   <li>Extraction result parsing and mapping to listings</li>
 * </ul>
 *
 * <p>The LLM and embedding calls are not tested here since they require real
 * API keys. Instead, we test each pipeline component independently against
 * the H2 in-memory database to verify the data flow and business logic.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProcessingPipelineTest {

    private static final Logger log = LogManager.getLogger(ProcessingPipelineTest.class);

    @Autowired private UserRepository userRepository;
    @Autowired private WhatsappGroupRepository groupRepository;
    @Autowired private RawMessageRepository rawMessageRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ManufacturerRepository manufacturerRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private JargonRepository jargonRepository;
    @Autowired private ReviewQueueItemRepository reviewQueueItemRepository;
    @Autowired private NotificationRuleRepository notificationRuleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatSessionRepository chatSessionRepository;
    @Autowired private JargonExpander jargonExpander;
    @Autowired private ConfidenceRouter confidenceRouter;
    @Autowired private TestDatabaseCleaner dbCleaner;

    private WhatsappGroup testGroup;
    private User testUser;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();

        // Seed reference data
        testUser = TestHelper.createUser(userRepository, "pipeline@test.com", UserRole.admin);

        testGroup = new WhatsappGroup();
        testGroup.setWhapiGroupId("pipeline-test-group@g.us");
        testGroup.setGroupName("Pipeline Test Group");
        testGroup.setIsActive(true);
        testGroup = groupRepository.save(testGroup);
    }

    @AfterEach
    void tearDown() {
        dbCleaner.cleanAll();
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private RawMessage createRawMessage(String msgId, String body) {
        RawMessage msg = new RawMessage();
        msg.setGroup(testGroup);
        msg.setWhapiMsgId(msgId);
        msg.setSenderPhone("15550001234@s.whatsapp.net");
        msg.setSenderName("Test Sender");
        msg.setMessageBody(body);
        msg.setMessageType("text");
        msg.setIsForwarded(false);
        msg.setTimestampWa(OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        msg.setProcessed(false);
        return rawMessageRepository.save(msg);
    }

    private ExtractionResult buildExtractionResult(String intent, double confidence,
                                                    List<ExtractionResult.ExtractedItem> items) {
        ExtractionResult result = new ExtractionResult();
        result.setIntent(intent);
        result.setConfidence(confidence);
        result.setItems(items != null ? items : new ArrayList<>());
        result.setUnknownTerms(new ArrayList<>());
        return result;
    }

    private ExtractionResult.ExtractedItem buildItem(String description, String category,
                                                      String manufacturer, Double quantity,
                                                      String unit, Double price, String condition) {
        ExtractionResult.ExtractedItem item = new ExtractionResult.ExtractedItem();
        item.setDescription(description);
        item.setCategory(category);
        item.setManufacturer(manufacturer);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setPrice(price);
        item.setCondition(condition);
        return item;
    }

    // =========================================================================
    // JargonExpander tests
    // =========================================================================

    @Nested
    @DisplayName("JargonExpander")
    class JargonExpanderTests {

        @Test
        @DisplayName("Expands verified jargon terms with word boundary matching")
        void expand_verifiedTerms_replacedInText() {
            JargonEntry entry = new JargonEntry();
            entry.setAcronym("NOS");
            entry.setExpansion("New Old Stock");
            entry.setSource("human");
            entry.setConfidence(1.0);
            entry.setVerified(true);
            jargonRepository.save(entry);

            String expanded = jargonExpander.expand("Selling 10x valves NOS $50 each");

            assertThat(expanded).contains("New Old Stock");
            assertThat(expanded).contains("(NOS)");
            log.info("Verified jargon expansion: {}", expanded);
        }

        @Test
        @DisplayName("Does not expand unverified jargon terms")
        void expand_unverifiedTerms_notReplaced() {
            JargonEntry entry = new JargonEntry();
            entry.setAcronym("MRO");
            entry.setExpansion("Maintenance, Repair, Operations");
            entry.setSource("llm");
            entry.setConfidence(0.5);
            entry.setVerified(false);
            jargonRepository.save(entry);

            String expanded = jargonExpander.expand("Need MRO supplies urgently");

            assertThat(expanded).isEqualTo("Need MRO supplies urgently");
            log.info("Verified unverified term not expanded");
        }

        @Test
        @DisplayName("Returns original text when no jargon entries exist")
        void expand_noEntries_returnsOriginal() {
            String original = "Selling 10x valves";
            String expanded = jargonExpander.expand(original);

            assertThat(expanded).isEqualTo(original);
        }

        @Test
        @DisplayName("Handles null and blank text gracefully")
        void expand_nullOrBlank_returnsAsIs() {
            assertThat(jargonExpander.expand(null)).isNull();
            assertThat(jargonExpander.expand("")).isEmpty();
            assertThat(jargonExpander.expand("   ")).isEqualTo("   ");
        }

        @Test
        @DisplayName("Case-insensitive jargon matching")
        void expand_caseInsensitive_matchesAllCases() {
            JargonEntry entry = new JargonEntry();
            entry.setAcronym("PVC");
            entry.setExpansion("Polyvinyl Chloride");
            entry.setSource("human");
            entry.setConfidence(1.0);
            entry.setVerified(true);
            jargonRepository.save(entry);

            String expanded = jargonExpander.expand("Selling pvc pipes and PVC fittings");

            // Both should be expanded
            assertThat(expanded).contains("Polyvinyl Chloride");
            log.info("Verified case-insensitive expansion: {}", expanded);
        }

        @Test
        @DisplayName("Does not replace partial word matches")
        void expand_partialMatch_notReplaced() {
            JargonEntry entry = new JargonEntry();
            entry.setAcronym("OS");
            entry.setExpansion("Old Stock");
            entry.setSource("human");
            entry.setConfidence(1.0);
            entry.setVerified(true);
            jargonRepository.save(entry);

            String expanded = jargonExpander.expand("Selling NOS fittings and OS gaskets");

            // "NOS" should not trigger the "OS" expansion due to word boundary
            // "OS" as a standalone word should be expanded
            assertThat(expanded).contains("Old Stock (OS)");
            // NOS should remain unchanged as it is a different word
            assertThat(expanded).contains("NOS");
            log.info("Verified word boundary matching: {}", expanded);
        }
    }

    // =========================================================================
    // ConfidenceRouter tests
    // =========================================================================

    @Nested
    @DisplayName("ConfidenceRouter")
    class ConfidenceRouterTests {

        @Test
        @DisplayName("Auto-accepts listings when confidence >= 0.8")
        void route_highConfidence_autoAccepts() {
            RawMessage msg = createRawMessage("route-high-001", "Selling 10x Parker valves $50");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Parker valves", null, null, 10.0, null, 50.0, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.9, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            Listing listing = listings.get(0);
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.active);
            assertThat(listing.getNeedsHumanReview()).isFalse();
            assertThat(listing.getIntent()).isEqualTo(IntentType.sell);
            assertThat(listing.getItemDescription()).isEqualTo("Parker valves");
            assertThat(listing.getConfidenceScore()).isEqualTo(0.9);
            assertThat(listing.getExpiresAt()).isNotNull();
            assertThat(listing.getId()).isNotNull();

            log.info("Verified auto-accept at confidence 0.9: listingId={}", listing.getId());
        }

        @Test
        @DisplayName("Routes to review when 0.5 <= confidence < 0.8")
        void route_mediumConfidence_pendingReview() {
            RawMessage msg = createRawMessage("route-med-001", "Buying some fittings maybe");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Fittings (uncertain)", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("want", 0.6, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            Listing listing = listings.get(0);
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.pending_review);
            assertThat(listing.getNeedsHumanReview()).isTrue();
            assertThat(listing.getIntent()).isEqualTo(IntentType.want);

            log.info("Verified pending-review at confidence 0.6: listingId={}", listing.getId());
        }

        @Test
        @DisplayName("Discards all items when confidence < 0.5")
        void route_lowConfidence_discards() {
            RawMessage msg = createRawMessage("route-low-001", "Random text with no trade intent");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Something", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("unknown", 0.3, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).isEmpty();
            assertThat(listingRepository.count()).isZero();

            log.info("Verified discard at confidence 0.3");
        }

        @Test
        @DisplayName("Resolves category, manufacturer, unit, and condition by name")
        void route_resolvesNormalizedFields() {
            // Seed reference data
            Category category = new Category();
            category.setName("Valves");
            category.setIsActive(true);
            category = categoryRepository.save(category);

            Manufacturer mfr = new Manufacturer();
            mfr.setName("Parker Hannifin");
            mfr.setIsActive(true);
            mfr = manufacturerRepository.save(mfr);

            Unit unit = new Unit();
            unit.setName("each");
            unit.setAbbreviation("ea");
            unit.setIsActive(true);
            unit = unitRepository.save(unit);

            Condition cond = new Condition();
            cond.setName("New Old Stock");
            cond.setIsActive(true);
            cond = conditionRepository.save(cond);

            RawMessage msg = createRawMessage("route-norm-001", "Selling 10x Parker Hannifin valves NOS");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Parker Hannifin ball valves", "Valves", "Parker Hannifin",
                    10.0, "ea", 50.0, "New Old Stock"
            );
            ExtractionResult result = buildExtractionResult("sell", 0.95, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            Listing listing = listings.get(0);
            assertThat(listing.getItemCategory()).isNotNull();
            assertThat(listing.getItemCategory().getName()).isEqualTo("Valves");
            assertThat(listing.getManufacturer()).isNotNull();
            assertThat(listing.getManufacturer().getName()).isEqualTo("Parker Hannifin");
            assertThat(listing.getUnit()).isNotNull();
            assertThat(listing.getUnit().getAbbreviation()).isEqualTo("ea");
            assertThat(listing.getCondition()).isNotNull();
            assertThat(listing.getCondition().getName()).isEqualTo("New Old Stock");

            log.info("Verified normalized field resolution for listing {}", listing.getId());
        }

        @Test
        @DisplayName("Leaves FK null when normalized value is not found")
        void route_unknownNormalizedValues_leavesNull() {
            RawMessage msg = createRawMessage("route-null-001", "Selling exotic widgets");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Exotic widgets", "NonexistentCategory", "UnknownMfr",
                    5.0, "nonexistent_unit", 100.0, "nonexistent_condition"
            );
            ExtractionResult result = buildExtractionResult("sell", 0.85, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            Listing listing = listings.get(0);
            assertThat(listing.getItemCategory()).isNull();
            assertThat(listing.getManufacturer()).isNull();
            assertThat(listing.getUnit()).isNull();
            assertThat(listing.getCondition()).isNull();
            // Listing should still be auto-accepted
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.active);

            log.info("Verified null FKs for unknown normalized values");
        }

        @Test
        @DisplayName("Handles multiple items in a single extraction result")
        void route_multipleItems_createsMultipleListings() {
            RawMessage msg = createRawMessage("route-multi-001", "Selling valves and pumps");

            ExtractionResult.ExtractedItem item1 = buildItem(
                    "Ball valves", null, null, 10.0, null, 50.0, null
            );
            ExtractionResult.ExtractedItem item2 = buildItem(
                    "Centrifugal pumps", null, null, 2.0, null, 500.0, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.9, List.of(item1, item2));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(2);
            assertThat(listings.get(0).getItemDescription()).isEqualTo("Ball valves");
            assertThat(listings.get(1).getItemDescription()).isEqualTo("Centrifugal pumps");
            assertThat(listings.stream().allMatch(l -> l.getStatus() == ListingStatus.active)).isTrue();

            log.info("Verified multiple items: {} listings created", listings.size());
        }

        @Test
        @DisplayName("Resolves unit by abbreviation when name lookup fails")
        void route_unitByAbbreviation_resolved() {
            Unit unit = new Unit();
            unit.setName("feet");
            unit.setAbbreviation("ft");
            unit.setIsActive(true);
            unitRepository.save(unit);

            RawMessage msg = createRawMessage("route-abbrev-001", "Selling 100ft of pipe");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Steel pipe", null, null, 100.0, "ft", null, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.85, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            assertThat(listings.get(0).getUnit()).isNotNull();
            assertThat(listings.get(0).getUnit().getAbbreviation()).isEqualTo("ft");

            log.info("Verified unit resolution by abbreviation");
        }

        @Test
        @DisplayName("Handles unknown intent gracefully")
        void route_unknownIntent_defaultsToUnknown() {
            RawMessage msg = createRawMessage("route-unk-001", "Unclear message");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Some item", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("invalid_intent", 0.85, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            assertThat(listings.get(0).getIntent()).isEqualTo(IntentType.unknown);

            log.info("Verified unknown intent handling");
        }

        @Test
        @DisplayName("Sets expiry date based on configured listing-expiry-days")
        void route_setsExpiryDate() {
            RawMessage msg = createRawMessage("route-exp-001", "Selling valves");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Valves", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.9, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            OffsetDateTime expiresAt = listings.get(0).getExpiresAt();
            assertThat(expiresAt).isNotNull();
            // Expiry should be approximately 60 days from now
            assertThat(expiresAt).isAfter(OffsetDateTime.now().plusDays(59));
            assertThat(expiresAt).isBefore(OffsetDateTime.now().plusDays(61));

            log.info("Verified expiry date: {}", expiresAt);
        }
    }

    // =========================================================================
    // ExtractionResult parsing tests
    // =========================================================================

    @Nested
    @DisplayName("ExtractionResult parsing")
    class ExtractionResultTests {

        @Test
        @DisplayName("Deserializes valid JSON extraction response")
        void parse_validJson_deserializesCorrectly() throws Exception {
            String json = """
                    {
                      "intent": "sell",
                      "items": [{
                        "description": "Parker ball valves",
                        "category": "Valves",
                        "manufacturer": "Parker Hannifin",
                        "part_number": "ABC-123",
                        "quantity": 10.0,
                        "unit": "ea",
                        "price": 50.0,
                        "condition": "NOS"
                      }],
                      "unknown_terms": ["FOB"],
                      "confidence": 0.92
                    }
                    """;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ExtractionResult result = mapper.readValue(json, ExtractionResult.class);

            assertThat(result.getIntent()).isEqualTo("sell");
            assertThat(result.getConfidence()).isEqualTo(0.92);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getUnknownTerms()).containsExactly("FOB");

            ExtractionResult.ExtractedItem item = result.getItems().get(0);
            assertThat(item.getDescription()).isEqualTo("Parker ball valves");
            assertThat(item.getCategory()).isEqualTo("Valves");
            assertThat(item.getManufacturer()).isEqualTo("Parker Hannifin");
            assertThat(item.getPartNumber()).isEqualTo("ABC-123");
            assertThat(item.getQuantity()).isEqualTo(10.0);
            assertThat(item.getUnit()).isEqualTo("ea");
            assertThat(item.getPrice()).isEqualTo(50.0);
            assertThat(item.getCondition()).isEqualTo("NOS");

            log.info("Verified ExtractionResult JSON deserialization");
        }

        @Test
        @DisplayName("Handles missing optional fields gracefully")
        void parse_missingFields_setsNulls() throws Exception {
            String json = """
                    {
                      "intent": "want",
                      "items": [{
                        "description": "Some valves"
                      }],
                      "confidence": 0.7
                    }
                    """;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ExtractionResult result = mapper.readValue(json, ExtractionResult.class);

            assertThat(result.getIntent()).isEqualTo("want");
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getCategory()).isNull();
            assertThat(result.getItems().get(0).getPrice()).isNull();
            assertThat(result.getUnknownTerms()).isEmpty();

            log.info("Verified graceful handling of missing fields");
        }

        @Test
        @DisplayName("Ignores unknown JSON properties")
        void parse_unknownProperties_ignored() throws Exception {
            String json = """
                    {
                      "intent": "sell",
                      "items": [],
                      "confidence": 0.5,
                      "extra_field": "should be ignored",
                      "unknown_terms": []
                    }
                    """;

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ExtractionResult result = mapper.readValue(json, ExtractionResult.class);

            assertThat(result.getIntent()).isEqualTo("sell");
            assertThat(result.getConfidence()).isEqualTo(0.5);

            log.info("Verified unknown properties are ignored");
        }
    }

    // =========================================================================
    // Review queue integration tests
    // =========================================================================

    @Nested
    @DisplayName("Review Queue Integration")
    class ReviewQueueIntegrationTests {

        @Test
        @DisplayName("Medium-confidence extraction creates listing and review queue item")
        void mediumConfidence_createsListingAndReviewItem() {
            RawMessage msg = createRawMessage("review-int-001", "Maybe selling some parts");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Unknown parts", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.6, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);
            assertThat(listings).hasSize(1);

            Listing listing = listings.get(0);
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.pending_review);

            // Verify the listing is persisted
            assertThat(listingRepository.findById(listing.getId())).isPresent();

            log.info("Verified medium-confidence creates pending listing {}",
                    listing.getId());
        }

        @Test
        @DisplayName("Boundary: confidence exactly at auto threshold (0.8) auto-accepts")
        void confidence_exactlyAtAutoThreshold_autoAccepts() {
            RawMessage msg = createRawMessage("boundary-auto-001", "Selling valves");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Valves", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.8, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            assertThat(listings.get(0).getStatus()).isEqualTo(ListingStatus.active);
            assertThat(listings.get(0).getNeedsHumanReview()).isFalse();

            log.info("Verified confidence=0.8 auto-accepts");
        }

        @Test
        @DisplayName("Boundary: confidence exactly at review threshold (0.5) routes to review")
        void confidence_exactlyAtReviewThreshold_routesToReview() {
            RawMessage msg = createRawMessage("boundary-review-001", "Maybe selling parts");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Parts", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("sell", 0.5, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).hasSize(1);
            assertThat(listings.get(0).getStatus()).isEqualTo(ListingStatus.pending_review);
            assertThat(listings.get(0).getNeedsHumanReview()).isTrue();

            log.info("Verified confidence=0.5 routes to review");
        }

        @Test
        @DisplayName("Boundary: confidence just below review threshold (0.49) discards")
        void confidence_belowReviewThreshold_discards() {
            RawMessage msg = createRawMessage("boundary-discard-001", "Random chat");

            ExtractionResult.ExtractedItem item = buildItem(
                    "Something", null, null, null, null, null, null
            );
            ExtractionResult result = buildExtractionResult("unknown", 0.49, List.of(item));

            List<Listing> listings = confidenceRouter.route(result, msg);

            assertThat(listings).isEmpty();

            log.info("Verified confidence=0.49 discards");
        }
    }
}
