package com.tradeintel.chat;

import com.tradeintel.admin.ChatMessageRepository;
import com.tradeintel.admin.ChatSessionRepository;
import com.tradeintel.admin.UsageLedgerRepository;
import com.tradeintel.chat.dto.ChatMessageDTO;
import com.tradeintel.chat.dto.ChatSessionDTO;
import com.tradeintel.chat.dto.ChatSessionDetailDTO;
import com.tradeintel.chat.dto.UserCostDTO;
import com.tradeintel.common.entity.ChatMessage;
import com.tradeintel.common.entity.ChatSession;
import com.tradeintel.common.entity.UsageLedger;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service managing chat sessions, message retrieval, and cost summaries.
 *
 * <p>Session creation, retrieval, and cost summary operations are handled
 * here, while the AI agent interaction logic resides in
 * {@link ChatAgentService}.</p>
 */
@Service
public class ChatService {

    private static final Logger log = LogManager.getLogger(ChatService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UsageLedgerRepository usageLedgerRepository;
    private final CostTrackingService costTrackingService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       UsageLedgerRepository usageLedgerRepository,
                       CostTrackingService costTrackingService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.usageLedgerRepository = usageLedgerRepository;
        this.costTrackingService = costTrackingService;
    }

    /**
     * Creates a new chat session for the given user.
     *
     * @param user  the session owner
     * @param title optional display title
     * @return the created session DTO
     */
    @Transactional
    public ChatSessionDTO createSession(User user, String title) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTitle(title != null && !title.isBlank() ? title : "New Chat");

        ChatSession saved = sessionRepository.save(session);
        costTrackingService.incrementSessionCount(user);

        log.info("Created chat session id={} for user={}", saved.getId(), user.getId());
        return ChatSessionDTO.fromEntity(saved);
    }

    /**
     * Retrieves a paginated list of chat sessions for the given user.
     *
     * @param userId   the user's UUID
     * @param pageable pagination parameters
     * @return page of session DTOs
     */
    @Transactional(readOnly = true)
    public Page<ChatSessionDTO> getUserSessions(UUID userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(ChatSessionDTO::fromEntity);
    }

    /**
     * Retrieves a session with its full message history. Validates ownership.
     *
     * @param sessionId the session UUID
     * @param userId    the requesting user's UUID
     * @return the session detail DTO with messages
     */
    @Transactional(readOnly = true)
    public ChatSessionDetailDTO getSessionDetail(UUID sessionId, UUID userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("ChatSession", sessionId);
        }

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<ChatMessageDTO> messageDTOs = messages.stream()
                .map(ChatMessageDTO::fromEntity)
                .toList();

        return new ChatSessionDetailDTO(
                session.getId(),
                session.getTitle(),
                messageDTOs,
                session.getCreatedAt()
        );
    }

    /**
     * Retrieves the user's cost summary with daily breakdown.
     *
     * @param userId the user's UUID
     * @return the user cost DTO
     */
    @Transactional(readOnly = true)
    public UserCostDTO getUserCostSummary(UUID userId) {
        Page<UsageLedger> page = usageLedgerRepository.findByUserIdOrderByPeriodDateDesc(
                userId, Pageable.unpaged());

        List<UsageLedger> entries = page.getContent();

        long totalInputTokens = 0L;
        long totalOutputTokens = 0L;
        BigDecimal totalCostUsd = BigDecimal.ZERO;
        int totalSessionCount = 0;

        List<UserCostDTO.DailyUsage> dailyBreakdown = new java.util.ArrayList<>();

        for (UsageLedger entry : entries) {
            totalInputTokens += entry.getTotalInputTokens();
            totalOutputTokens += entry.getTotalOutputTokens();
            totalCostUsd = totalCostUsd.add(entry.getTotalCostUsd());
            totalSessionCount += entry.getSessionCount();

            dailyBreakdown.add(new UserCostDTO.DailyUsage(
                    entry.getPeriodDate(),
                    entry.getTotalInputTokens(),
                    entry.getTotalOutputTokens(),
                    entry.getTotalCostUsd()
            ));
        }

        return new UserCostDTO(
                totalInputTokens,
                totalOutputTokens,
                totalCostUsd,
                totalSessionCount,
                dailyBreakdown
        );
    }
}
