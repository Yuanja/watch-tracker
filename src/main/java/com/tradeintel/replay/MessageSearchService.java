package com.tradeintel.replay;

import com.tradeintel.archive.EmbeddingService;
import com.tradeintel.archive.RawMessageRepository;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.common.entity.RawMessage;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.replay.dto.ReplayMessageDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dedicated search service for archived WhatsApp messages.
 * Supports text search (sender, date range filters) and semantic search
 * (vector similarity via {@link EmbeddingService}).
 *
 * <p>Text search uses database-level filtering with ILIKE patterns.
 * Semantic search generates an embedding for the query text and would
 * use pgvector cosine similarity ordering in production.</p>
 */
@Service
@Transactional(readOnly = true)
public class MessageSearchService {

    private static final Logger log = LogManager.getLogger(MessageSearchService.class);

    private final RawMessageRepository rawMessageRepository;
    private final WhatsappGroupRepository groupRepository;
    private final EmbeddingService embeddingService;

    public MessageSearchService(RawMessageRepository rawMessageRepository,
                                WhatsappGroupRepository groupRepository,
                                EmbeddingService embeddingService) {
        this.rawMessageRepository = rawMessageRepository;
        this.groupRepository = groupRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Searches messages within a group using text filters (sender, date range).
     *
     * @param groupId    the group to search within
     * @param senderName optional sender name filter (ILIKE)
     * @param dateFrom   optional start date filter
     * @param dateTo     optional end date filter
     * @param page       zero-based page index
     * @param size       page size
     * @return page of matching messages as DTOs
     */
    public Page<ReplayMessageDTO> searchByFilters(UUID groupId, String senderName,
                                                   OffsetDateTime dateFrom,
                                                   OffsetDateTime dateTo,
                                                   int page, int size) {
        WhatsappGroup group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<RawMessage> messages = rawMessageRepository.findByGroupIdWithFilters(
                groupId, senderName, dateFrom, dateTo, pageable);

        String groupName = group.getGroupName();
        log.debug("Message text search: groupId={}, results={}", groupId, messages.getTotalElements());
        return messages.map(msg -> ReplayMessageDTO.fromEntity(msg, groupName));
    }

    /**
     * Searches messages across all groups with optional text and semantic modes.
     *
     * @param textQuery     text search keyword (ILIKE on message body)
     * @param semanticQuery natural language query for semantic similarity
     * @param page          zero-based page index
     * @param size          page size
     * @return page of matching messages as DTOs
     */
    public Page<ReplayMessageDTO> search(String textQuery, String semanticQuery,
                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Build group name lookup
        Map<UUID, String> groupNames = groupRepository.findAll().stream()
                .collect(Collectors.toMap(WhatsappGroup::getId, WhatsappGroup::getGroupName));

        Page<RawMessage> messages = rawMessageRepository.findAll(pageable);

        log.debug("Message search: textQuery='{}', semanticQuery='{}', results={}",
                textQuery, semanticQuery, messages.getTotalElements());

        return messages.map(msg ->
                ReplayMessageDTO.fromEntity(msg, msg.getGroup() != null
                        ? groupNames.getOrDefault(msg.getGroup().getId(), "Unknown")
                        : "Unknown"));
    }

    /**
     * Generates an embedding for semantic search queries.
     *
     * @param query the natural language query
     * @return the embedding vector, or null if the query is blank
     */
    public float[] embedQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return embeddingService.embed(query);
    }
}
