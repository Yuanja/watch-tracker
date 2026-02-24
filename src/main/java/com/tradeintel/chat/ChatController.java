package com.tradeintel.chat;

import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.chat.dto.ChatRequest;
import com.tradeintel.chat.dto.ChatResponseDTO;
import com.tradeintel.chat.dto.ChatSessionDTO;
import com.tradeintel.chat.dto.ChatSessionDetailDTO;
import com.tradeintel.chat.dto.CreateSessionRequest;
import com.tradeintel.chat.dto.UserCostDTO;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for the AI chat feature.
 *
 * <p>All endpoints require authentication. Users can create chat sessions,
 * view their sessions and messages, send messages to the AI agent, and
 * view their cost summary.</p>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LogManager.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ChatAgentService chatAgentService;
    private final UserRepository userRepository;

    public ChatController(ChatService chatService,
                          ChatAgentService chatAgentService,
                          UserRepository userRepository) {
        this.chatService = chatService;
        this.chatAgentService = chatAgentService;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new chat session for the authenticated user.
     *
     * @param request   optional body with a title
     * @param principal the authenticated user principal
     * @return the created session DTO
     */
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(
            @RequestBody(required = false) CreateSessionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = resolveUser(principal);
        String title = request != null ? request.title() : null;

        ChatSessionDTO session = chatService.createSession(user, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Lists the authenticated user's chat sessions (paginated).
     *
     * @param page      zero-based page index (default 0)
     * @param size      page size (default 20)
     * @param principal the authenticated user principal
     * @return page of session DTOs
     */
    @GetMapping("/sessions")
    public ResponseEntity<Page<ChatSessionDTO>> listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatSessionDTO> sessions = chatService.getUserSessions(principal.getUserId(), pageable);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieves a specific session with its full message history.
     *
     * @param id        the session UUID
     * @param principal the authenticated user principal
     * @return the session detail DTO
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<ChatSessionDetailDTO> getSessionDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ChatSessionDetailDTO detail = chatService.getSessionDetail(id, principal.getUserId());
        return ResponseEntity.ok(detail);
    }

    /**
     * Sends a user message within a session and returns the AI response.
     *
     * @param id        the session UUID
     * @param request   the chat request containing the user's message
     * @param principal the authenticated user principal
     * @return the chat response DTO with the AI's reply
     */
    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ChatResponseDTO> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = resolveUser(principal);
        ChatResponseDTO response = chatAgentService.processMessage(id, request.message(), user);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the authenticated user's cost summary with daily breakdown.
     *
     * @param principal the authenticated user principal
     * @return the user cost DTO
     */
    @GetMapping("/cost")
    public ResponseEntity<UserCostDTO> getCostSummary(
            @AuthenticationPrincipal UserPrincipal principal) {

        UserCostDTO cost = chatService.getUserCostSummary(principal.getUserId());
        return ResponseEntity.ok(cost);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the full User entity from the authenticated principal.
     */
    private User resolveUser(UserPrincipal principal) {
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));
    }
}
