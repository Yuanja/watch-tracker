package com.tradeintel.webhook;

import com.tradeintel.archive.MessageArchiveService;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.event.NewMessageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WhapiWebhookController {

    private static final Logger log = LogManager.getLogger(WhapiWebhookController.class);

    private final MessageArchiveService archiveService;
    private final ApplicationEventPublisher eventPublisher;
    private final String webhookSecret;

    public WhapiWebhookController(MessageArchiveService archiveService,
                                  ApplicationEventPublisher eventPublisher,
                                  @Value("${app.whapi.webhook-secret}") String webhookSecret) {
        this.archiveService = archiveService;
        this.eventPublisher = eventPublisher;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/whapi")
    public ResponseEntity<Void> receiveMessage(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Whapi-Signature", required = false) String signature) {

        if (!WhapiSignatureValidator.isValid(rawBody, signature, webhookSecret)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.status(401).build();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            WhapiMessageDTO payload = mapper.readValue(rawBody, WhapiMessageDTO.class);

            if (payload.getMessages() == null || payload.getMessages().isEmpty()) {
                return ResponseEntity.ok().build();
            }

            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(rawBody);
            com.fasterxml.jackson.databind.JsonNode messagesNode = root.get("messages");

            for (int i = 0; i < payload.getMessages().size(); i++) {
                WhapiMessageDTO.Message msg = payload.getMessages().get(i);
                String msgJson = (messagesNode != null && messagesNode.has(i))
                        ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messagesNode.get(i))
                        : null;
                RawMessage saved = archiveService.archive(msg, msgJson);
                if (saved != null) {
                    log.info("Archived message: {}", saved.getWhapiMsgId());
                    eventPublisher.publishEvent(new NewMessageEvent(saved.getId()));
                }
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
