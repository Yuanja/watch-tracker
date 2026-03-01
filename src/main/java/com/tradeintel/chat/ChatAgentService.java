package com.tradeintel.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeintel.admin.ChatMessageRepository;
import com.tradeintel.admin.ChatSessionRepository;
import com.tradeintel.chat.dto.ChatMessageDTO;
import com.tradeintel.chat.dto.ChatResponseDTO;
import com.tradeintel.chat.tools.CreateNotificationTool;
import com.tradeintel.chat.tools.GetListingDetailsTool;
import com.tradeintel.chat.tools.MarketStatsTool;
import com.tradeintel.chat.tools.SearchListingsTool;
import com.tradeintel.chat.tools.SearchMessagesTool;
import com.tradeintel.common.entity.ChatMessage;
import com.tradeintel.common.entity.ChatSession;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.common.openai.OpenAIClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI agent service that implements the chat interaction loop with tool calling.
 *
 * <p>The agent flow is:
 * <ol>
 *   <li>Load session history and build message context</li>
 *   <li>Call OpenAI chat completion with the conversation</li>
 *   <li>If the response contains a tool call JSON block, extract and execute it</li>
 *   <li>Feed the tool result back to the LLM for a final response</li>
 *   <li>Save messages and track costs</li>
 * </ol>
 *
 * <p>Tool calls are detected by looking for JSON blocks containing a "tool" field
 * in the assistant's response. This is a single-turn tool pattern: at most one
 * tool call per user message.</p>
 */
@Service
public class ChatAgentService {

    private static final Logger log = LogManager.getLogger(ChatAgentService.class);

    /** Regex pattern to find JSON tool call blocks in the assistant response. */
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"params\"\\s*:\\s*(\\{[^}]*\\})\\s*\\}",
            Pattern.DOTALL);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final OpenAIClient openAIClient;
    private final CostTrackingService costTrackingService;
    private final SearchListingsTool searchListingsTool;
    private final SearchMessagesTool searchMessagesTool;
    private final MarketStatsTool marketStatsTool;
    private final CreateNotificationTool createNotificationTool;
    private final GetListingDetailsTool getListingDetailsTool;
    private final ObjectMapper objectMapper;
    private final String chatModel;
    private final String systemPrompt;

    public ChatAgentService(ChatSessionRepository sessionRepository,
                            ChatMessageRepository messageRepository,
                            OpenAIClient openAIClient,
                            CostTrackingService costTrackingService,
                            SearchListingsTool searchListingsTool,
                            SearchMessagesTool searchMessagesTool,
                            MarketStatsTool marketStatsTool,
                            CreateNotificationTool createNotificationTool,
                            GetListingDetailsTool getListingDetailsTool,
                            ObjectMapper objectMapper,
                            @Value("${app.openai.chat-model}") String chatModel) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.openAIClient = openAIClient;
        this.costTrackingService = costTrackingService;
        this.searchListingsTool = searchListingsTool;
        this.searchMessagesTool = searchMessagesTool;
        this.marketStatsTool = marketStatsTool;
        this.createNotificationTool = createNotificationTool;
        this.getListingDetailsTool = getListingDetailsTool;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
        this.systemPrompt = loadSystemPrompt();
    }

    /**
     * Processes a user message within a chat session and returns the AI response.
     *
     * @param sessionId   the chat session UUID
     * @param userMessage the user's message text
     * @param user        the authenticated user
     * @return the chat response DTO with the assistant's message and any tool results
     */
    @Transactional
    public ChatResponseDTO processMessage(UUID sessionId, String userMessage, User user) {
        // Load and validate session ownership
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("ChatSession", sessionId);
        }

        // Save the user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        // Build conversation history for the LLM
        List<OpenAIClient.ChatMessage> conversationHistory = buildConversationHistory(sessionId);

        // First LLM call
        log.info("Calling LLM for session={}, model={}", sessionId, chatModel);
        OpenAIClient.ChatCompletionResponse firstResponse = openAIClient.chatCompletion(
                chatModel, conversationHistory, 0.7);

        int totalInputTokens = firstResponse.inputTokens();
        int totalOutputTokens = firstResponse.outputTokens();
        double totalCost = firstResponse.estimateCost();
        List<String> toolResults = new ArrayList<>();
        String assistantContent = firstResponse.content();
        String toolCallsJson = null;

        // Check for tool call in the response
        ToolCall toolCall = extractToolCall(assistantContent);
        if (toolCall != null) {
            log.info("Tool call detected: tool={}, params={}", toolCall.toolName, toolCall.params);

            // Execute the tool
            String toolResult = executeTool(toolCall.toolName, toolCall.params, user);
            toolResults.add(toolResult);

            try {
                toolCallsJson = objectMapper.writeValueAsString(List.of(Map.of(
                        "tool", toolCall.toolName,
                        "params", toolCall.params,
                        "result", toolResult
                )));
            } catch (Exception e) {
                log.warn("Failed to serialize tool calls JSON", e);
            }

            // Build a follow-up conversation with the tool result
            List<OpenAIClient.ChatMessage> followUpMessages = new ArrayList<>(conversationHistory);
            followUpMessages.add(new OpenAIClient.ChatMessage("assistant", assistantContent));
            followUpMessages.add(new OpenAIClient.ChatMessage("user",
                    "Tool result for " + toolCall.toolName + ":\n" + toolResult
                            + "\n\nPlease provide a helpful response based on this data."));

            // Second LLM call with tool result
            log.info("Calling LLM again with tool result for session={}", sessionId);
            OpenAIClient.ChatCompletionResponse secondResponse = openAIClient.chatCompletion(
                    chatModel, followUpMessages, 0.7);

            totalInputTokens += secondResponse.inputTokens();
            totalOutputTokens += secondResponse.outputTokens();
            totalCost += secondResponse.estimateCost();
            assistantContent = secondResponse.content();
        }

        // Save assistant message
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSession(session);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantContent);
        assistantMsg.setModelUsed(chatModel);
        assistantMsg.setInputTokens(totalInputTokens);
        assistantMsg.setOutputTokens(totalOutputTokens);
        assistantMsg.setCostUsd(BigDecimal.valueOf(totalCost));
        assistantMsg.setToolCalls(toolCallsJson);
        ChatMessage savedAssistant = messageRepository.save(assistantMsg);

        // Track cost
        costTrackingService.trackUsage(user, chatModel, totalInputTokens, totalOutputTokens, totalCost);

        // Update session timestamp
        session.setUpdatedAt(java.time.OffsetDateTime.now());
        sessionRepository.save(session);

        log.info("Chat response for session={}: inputTokens={}, outputTokens={}, cost={}, toolsCalled={}",
                sessionId, totalInputTokens, totalOutputTokens, totalCost, toolResults.size());

        return new ChatResponseDTO(
                ChatMessageDTO.fromEntity(savedAssistant),
                toolResults.isEmpty() ? List.of() : toolResults
        );
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the full conversation history for the LLM including the system
     * prompt and all previous messages in the session.
     */
    private List<OpenAIClient.ChatMessage> buildConversationHistory(UUID sessionId) {
        List<OpenAIClient.ChatMessage> messages = new ArrayList<>();

        // System prompt
        messages.add(new OpenAIClient.ChatMessage("system", systemPrompt));

        // Previous messages
        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        for (ChatMessage msg : history) {
            messages.add(new OpenAIClient.ChatMessage(msg.getRole(), msg.getContent()));
        }

        return messages;
    }

    /**
     * Extracts a tool call from the assistant's response by looking for JSON
     * blocks containing a "tool" field.
     */
    private ToolCall extractToolCall(String response) {
        if (response == null) {
            return null;
        }

        // Try regex pattern first
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        if (matcher.find()) {
            String toolName = matcher.group(1);
            String paramsJson = matcher.group(2);
            try {
                Map<String, Object> params = objectMapper.readValue(
                        paramsJson, new TypeReference<Map<String, Object>>() {});
                return new ToolCall(toolName, params);
            } catch (Exception e) {
                log.warn("Failed to parse tool call params: {}", paramsJson, e);
            }
        }

        // Try to find a JSON block with "tool" field anywhere in the response
        try {
            int braceStart = response.indexOf('{');
            while (braceStart >= 0) {
                int braceEnd = findMatchingBrace(response, braceStart);
                if (braceEnd > braceStart) {
                    String jsonCandidate = response.substring(braceStart, braceEnd + 1);
                    JsonNode node = objectMapper.readTree(jsonCandidate);
                    if (node.has("tool") && node.has("params")) {
                        String toolName = node.get("tool").asText();
                        Map<String, Object> params = objectMapper.convertValue(
                                node.get("params"), new TypeReference<Map<String, Object>>() {});
                        return new ToolCall(toolName, params);
                    }
                }
                braceStart = response.indexOf('{', braceStart + 1);
            }
        } catch (Exception e) {
            // No valid tool call found
            log.debug("No tool call JSON found in response");
        }

        return null;
    }

    /**
     * Finds the matching closing brace for an opening brace at the given position.
     */
    private int findMatchingBrace(String text, int openPos) {
        int depth = 0;
        boolean inString = false;
        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Executes the named tool with the given parameters.
     */
    private String executeTool(String toolName, Map<String, Object> params, User user) {
        return switch (toolName) {
            case "search_listings" -> searchListingsTool.execute(params);
            case "search_messages" -> searchMessagesTool.execute(params);
            case "market_stats" -> marketStatsTool.execute(params);
            case "create_notification" -> createNotificationTool.execute(params, user);
            case "get_listing_details" -> getListingDetailsTool.execute(params);
            default -> {
                log.warn("Unknown tool requested: {}", toolName);
                yield "{\"error\": \"Unknown tool: " + toolName + "\"}";
            }
        };
    }

    /**
     * Loads the chat system prompt from the classpath resource.
     */
    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/chat_system.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load chat_system.txt prompt", e);
            return "You are a helpful AI assistant for DialIntel.ai, a watch market intelligence platform.";
        }
    }

    /**
     * Internal record for a parsed tool call.
     */
    private record ToolCall(String toolName, Map<String, Object> params) {}
}
