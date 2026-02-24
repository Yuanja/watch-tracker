package com.tradeintel.notification;

import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.notification.dto.CreateRuleRequest;
import com.tradeintel.notification.dto.NotificationRuleDTO;
import com.tradeintel.notification.dto.UpdateRuleRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing user notification rules.
 *
 * <p>All endpoints require authentication. Each user can only manage their
 * own rules; ownership is enforced by the service layer.</p>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LogManager.getLogger(NotificationController.class);

    private final NotificationRuleService ruleService;
    private final UserRepository userRepository;

    public NotificationController(NotificationRuleService ruleService,
                                  UserRepository userRepository) {
        this.ruleService = ruleService;
        this.userRepository = userRepository;
    }

    /**
     * Lists all notification rules belonging to the authenticated user.
     *
     * @param principal the authenticated user principal
     * @return list of notification rule DTOs
     */
    @GetMapping
    public ResponseEntity<List<NotificationRuleDTO>> listRules(
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal.getUserId();
        List<NotificationRuleDTO> rules = ruleService.listUserRules(userId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Creates a new notification rule from natural language.
     *
     * @param request   the create request containing the NL rule text
     * @param principal the authenticated user principal
     * @return the created rule DTO
     */
    @PostMapping
    public ResponseEntity<NotificationRuleDTO> createRule(
            @Valid @RequestBody CreateRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));

        NotificationRuleDTO dto = ruleService.createRule(user, request.nlRule(), request.notifyEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Updates an existing notification rule.
     *
     * @param id        the rule UUID
     * @param request   the update request
     * @param principal the authenticated user principal
     * @return the updated rule DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationRuleDTO> updateRule(
            @PathVariable UUID id,
            @RequestBody UpdateRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationRuleDTO dto = ruleService.updateRule(id, principal.getUserId(), request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Deletes a notification rule.
     *
     * @param id        the rule UUID
     * @param principal the authenticated user principal
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ruleService.deleteRule(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
