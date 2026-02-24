package com.tradeintel;

import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for {@link com.tradeintel.webhook.WhapiWebhookController}.
 *
 * <p>Tests verify the full webhook processing path: HMAC signature validation,
 * idempotent message archival, and auto-creation of {@link com.tradeintel.common.entity.WhatsappGroup}
 * records for previously unseen chat IDs.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebhookControllerTest {

    private static final Logger log = LogManager.getLogger(WebhookControllerTest.class);

    /** Matches {@code app.whapi.webhook-secret} in test application.yml. */
    private static final String TEST_WEBHOOK_SECRET = "test-webhook-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RawMessageRepository rawMessageRepository;

    @Autowired
    private WhatsappGroupRepository whatsappGroupRepository;

    @BeforeEach
    void setUp() {
        // Start each test with an empty archive so counts are deterministic.
        rawMessageRepository.deleteAll();
        whatsappGroupRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildValidPayload(String msgId, String chatId, String body) {
        return """
                {
                  "messages": [
                    {
                      "id": "%s",
                      "from": "15551234567@s.whatsapp.net",
                      "chat_id": "%s",
                      "from_name": "Alice",
                      "timestamp": 1700000000,
                      "text": { "body": "%s" }
                    }
                  ]
                }
                """.formatted(msgId, chatId, body);
    }

    private String signedHeader(String body) {
        return TestHelper.computeHmacSignature(body, TEST_WEBHOOK_SECRET);
    }

    // -------------------------------------------------------------------------
    // Signature validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/webhooks/whapi returns 200 for a valid signed message")
    void receiveMessage_validSignature_returns200() throws Exception {
        String payload = buildValidPayload("msg-valid-001", "group-a@g.us", "Selling 10x valves");
        String signature = signedHeader(payload);

        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        log.info("Verified valid webhook returns 200");
    }

    @Test
    @DisplayName("POST /api/webhooks/whapi returns 401 for a missing signature")
    void receiveMessage_missingSignature_returns401() throws Exception {
        String payload = buildValidPayload("msg-nosig-001", "group-a@g.us", "No signature");

        // No X-Whapi-Signature header.
        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        log.info("Verified missing signature returns 401");
    }

    @Test
    @DisplayName("POST /api/webhooks/whapi returns 401 for a tampered signature")
    void receiveMessage_invalidSignature_returns401() throws Exception {
        String payload = buildValidPayload("msg-badsig-001", "group-a@g.us", "Bad signature");

        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", "0000000000000000000000000000000000000000000000000000000000000000")
                        .content(payload))
                .andExpect(status().isUnauthorized());

        log.info("Verified invalid signature returns 401");
    }

    // -------------------------------------------------------------------------
    // Message archival
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Valid webhook archives the message and auto-creates the group")
    void receiveMessage_validPayload_archivesMessageAndCreatesGroup() throws Exception {
        String chatId  = "group-archive@g.us";
        String msgId   = "msg-archive-001";
        String payload = buildValidPayload(msgId, chatId, "Buying 5x pumps");
        String signature = signedHeader(payload);

        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        // Group should have been auto-created.
        assertThat(whatsappGroupRepository.findByWhapiGroupId(chatId))
                .isPresent()
                .hasValueSatisfying(g -> assertThat(g.getIsActive()).isTrue());

        // Message should have been archived.
        assertThat(rawMessageRepository.findByWhapiMsgId(msgId))
                .isPresent()
                .hasValueSatisfying(m -> {
                    assertThat(m.getMessageBody()).isEqualTo("Buying 5x pumps");
                    assertThat(m.getSenderPhone()).isEqualTo("15551234567@s.whatsapp.net");
                    assertThat(m.getSenderName()).isEqualTo("Alice");
                    assertThat(m.getProcessed()).isFalse();
                });

        log.info("Verified message archived with msgId={}", msgId);
    }

    // -------------------------------------------------------------------------
    // Idempotency
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sending the same webhook twice archives the message only once")
    void receiveMessage_duplicate_isIdempotent() throws Exception {
        String chatId  = "group-idem@g.us";
        String msgId   = "msg-idem-001";
        String payload = buildValidPayload(msgId, chatId, "Duplicate message test");
        String signature = signedHeader(payload);

        // First delivery.
        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        // Second delivery — same message ID.
        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        // Should still only have one row in the archive.
        long count = rawMessageRepository.findAll().stream()
                .filter(m -> msgId.equals(m.getWhapiMsgId()))
                .count();
        assertThat(count).isEqualTo(1);

        log.info("Verified duplicate message is idempotent; exactly one row persisted for msgId={}", msgId);
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Payload with empty messages list returns 200 without persisting anything")
    void receiveMessage_emptyMessagesList_returns200AndPersistsNothing() throws Exception {
        String payload = """
                { "messages": [] }
                """;
        String signature = signedHeader(payload);

        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(rawMessageRepository.count()).isZero();
    }

    @Test
    @DisplayName("Payload without a messages key returns 200 without persisting anything")
    void receiveMessage_missingMessagesKey_returns200AndPersistsNothing() throws Exception {
        String payload = """
                { "event": "ping" }
                """;
        String signature = signedHeader(payload);

        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(rawMessageRepository.count()).isZero();
    }

    @Test
    @DisplayName("Multiple messages in one payload are all archived")
    void receiveMessage_multipleMessages_archivesAll() throws Exception {
        String payload = """
                {
                  "messages": [
                    {
                      "id": "msg-multi-001",
                      "from": "15550000001@s.whatsapp.net",
                      "chat_id": "group-multi@g.us",
                      "from_name": "Bob",
                      "timestamp": 1700000001,
                      "text": { "body": "Selling valves" }
                    },
                    {
                      "id": "msg-multi-002",
                      "from": "15550000002@s.whatsapp.net",
                      "chat_id": "group-multi@g.us",
                      "from_name": "Carol",
                      "timestamp": 1700000002,
                      "text": { "body": "Buying pumps" }
                    }
                  ]
                }
                """;
        String signature = signedHeader(payload);

        mockMvc.perform(post("/api/webhooks/whapi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Whapi-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(rawMessageRepository.count()).isEqualTo(2);
        // Both messages shared the same group — only one group row.
        assertThat(whatsappGroupRepository.count()).isEqualTo(1);

        log.info("Verified batch of 2 messages all archived");
    }
}
