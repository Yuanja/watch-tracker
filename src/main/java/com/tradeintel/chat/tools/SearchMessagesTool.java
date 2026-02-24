package com.tradeintel.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.common.entity.RawMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Chat tool that searches raw WhatsApp messages by text content,
 * group ID, or sender name.
 *
 * <p>Returns up to 10 matching messages formatted as a JSON array
 * suitable for inclusion in an LLM context.</p>
 */
@Component
public class SearchMessagesTool {

    private static final Logger log = LogManager.getLogger(SearchMessagesTool.class);
    private static final int MAX_RESULTS = 10;

    private final RawMessageRepository rawMessageRepository;
    private final ObjectMapper objectMapper;

    public SearchMessagesTool(RawMessageRepository rawMessageRepository, ObjectMapper objectMapper) {
        this.rawMessageRepository = rawMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the search_messages tool with the given parameters.
     *
     * @param params map of parameter names to values extracted from the LLM tool call
     * @return JSON string with up to 10 matching messages
     */
    public String execute(Map<String, Object> params) {
        try {
            UUID groupId = null;
            String sender = null;

            if (params.containsKey("groupId") && params.get("groupId") != null) {
                groupId = UUID.fromString(params.get("groupId").toString());
            }

            if (params.containsKey("sender") && params.get("sender") != null) {
                sender = params.get("sender").toString();
            }

            Page<RawMessage> results;

            if (groupId != null) {
                results = rawMessageRepository.findByGroupIdWithFilters(
                        groupId, sender, null, null,
                        PageRequest.of(0, MAX_RESULTS));
            } else {
                // Fall back to finding all messages with optional sender filter
                results = rawMessageRepository.findAll(PageRequest.of(0, MAX_RESULTS));
            }

            ArrayNode resultsArray = objectMapper.createArrayNode();
            for (RawMessage msg : results.getContent()) {
                // Apply text query filter in-memory if groupId was not specified
                if (params.containsKey("query") && params.get("query") != null && groupId == null) {
                    String query = params.get("query").toString().toLowerCase();
                    if (msg.getMessageBody() != null && !msg.getMessageBody().toLowerCase().contains(query)) {
                        continue;
                    }
                }

                ObjectNode node = resultsArray.addObject();
                node.put("sender", msg.getSenderName() != null ? msg.getSenderName() : "Unknown");
                node.put("body", msg.getMessageBody() != null
                        ? (msg.getMessageBody().length() > 500
                                ? msg.getMessageBody().substring(0, 500) + "..."
                                : msg.getMessageBody())
                        : "");
                node.put("timestamp", msg.getTimestampWa() != null ? msg.getTimestampWa().toString() : "");
                if (msg.getGroup() != null) {
                    node.put("group", msg.getGroup().getGroupName());
                }
                node.put("type", msg.getMessageType());
            }

            String json = objectMapper.writeValueAsString(resultsArray);
            log.info("SearchMessagesTool returned {} results for params={}", resultsArray.size(), params);
            return json;

        } catch (Exception e) {
            log.error("SearchMessagesTool failed", e);
            return "{\"error\": \"Search failed: " + e.getMessage() + "\"}";
        }
    }
}
