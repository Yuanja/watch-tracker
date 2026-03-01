package com.tradeintel.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Lightweight client for the Whapi.cloud REST API.
 * Used to resolve group/contact names from chat IDs.
 */
@Component
public class WhapiApiClient {

    private static final Logger log = LogManager.getLogger(WhapiApiClient.class);
    private static final String BASE_URL = "https://gate.whapi.cloud";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public WhapiApiClient(@Value("${app.whapi.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    /**
     * Resolves a friendly name for a chat ID by calling the Whapi API.
     * For group chats (ending in @g.us), calls the groups endpoint.
     * For personal chats (ending in @s.whatsapp.net), calls the contacts endpoint.
     *
     * @param chatId the Whapi chat ID
     * @return the friendly name, or null if resolution failed
     */
    public GroupInfo resolveGroupInfo(String chatId) {
        if (apiKey == null || apiKey.isBlank() || "placeholder".equals(apiKey)) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url;
            if (chatId.endsWith("@g.us")) {
                url = BASE_URL + "/groups/" + chatId;
            } else {
                // Personal chat — strip the @s.whatsapp.net suffix
                String phone = chatId.replace("@s.whatsapp.net", "");
                url = BASE_URL + "/contacts/" + phone;
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String name = root.path("name").asText(null);
                String avatar = root.path("chat_pic").asText(null);
                if (avatar == null) {
                    avatar = root.path("photo").asText(null);
                }

                if (name != null) {
                    log.info("Resolved chat {} -> name='{}' avatar={}", chatId, name, avatar != null);
                    return new GroupInfo(name, avatar);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve name for chat {}: {}", chatId, e.getMessage());
        }

        return null;
    }

    public record GroupInfo(String name, String avatarUrl) {}
}
