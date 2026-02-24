package com.tradeintel.common.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAIClient {

    private static final Logger log = LogManager.getLogger(OpenAIClient.class);
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAIClient(@Value("${app.openai.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
    }

    public ChatCompletionResponse chatCompletion(String model, List<ChatMessage> messages, double temperature) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (ChatMessage msg : messages) {
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(CHAT_URL, HttpMethod.POST, entity, String.class);
            JsonNode responseNode = objectMapper.readTree(response.getBody());

            String content = responseNode.path("choices").path(0).path("message").path("content").asText();
            int inputTokens = responseNode.path("usage").path("prompt_tokens").asInt();
            int outputTokens = responseNode.path("usage").path("completion_tokens").asInt();

            return new ChatCompletionResponse(content, model, inputTokens, outputTokens);

        } catch (Exception e) {
            log.error("OpenAI chat completion failed", e);
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    public float[] embed(String text, String model) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("input", text);

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(EMBEDDING_URL, HttpMethod.POST, entity, String.class);
            JsonNode responseNode = objectMapper.readTree(response.getBody());

            JsonNode embeddingArray = responseNode.path("data").path(0).path("embedding");
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = (float) embeddingArray.path(i).asDouble();
            }

            return embedding;

        } catch (Exception e) {
            log.error("OpenAI embedding failed", e);
            throw new RuntimeException("OpenAI embedding API call failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    public record ChatMessage(String role, String content) {}

    public record ChatCompletionResponse(String content, String model, int inputTokens, int outputTokens) {
        public double estimateCost() {
            double inputRate;
            double outputRate;
            if (model.contains("gpt-4o-mini")) {
                inputRate = 0.15 / 1_000_000;
                outputRate = 0.60 / 1_000_000;
            } else {
                inputRate = 2.50 / 1_000_000;
                outputRate = 10.00 / 1_000_000;
            }
            return inputTokens * inputRate + outputTokens * outputRate;
        }
    }
}
