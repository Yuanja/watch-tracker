package com.tradeintel.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeintel.admin.dto.ChatSessionDTO;
import com.tradeintel.admin.dto.UserDTO;
import com.tradeintel.auth.UserRepository;
import com.tradeintel.common.entity.User;
import com.tradeintel.common.entity.UserRole;
import com.tradeintel.common.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service for uber_admin user management operations.
 *
 * <p>Every mutating operation produces an audit log entry via {@link AuditService}
 * so that all role changes and account activations/deactivations are permanently
 * recorded with actor, target, before/after state, and client IP.
 */
@Service
public class UserManagementService {

    private static final Logger log = LogManager.getLogger(UserManagementService.class);

    private final UserRepository userRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public UserManagementService(UserRepository userRepository,
                                 ChatSessionRepository chatSessionRepository,
                                 AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated list of all platform users ordered by the supplied
     * {@link Pageable} parameters (default sort: by creation time).
     *
     * @param pageable pagination and sort parameters
     * @return page of {@link UserDTO} projected from JPA entities
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDTO::fromEntity);
    }

    /**
     * Returns a paginated list of chat sessions belonging to the specified user.
     * The user must exist; otherwise a {@link ResourceNotFoundException} is thrown.
     *
     * @param userId   the UUID of the target user
     * @param pageable pagination and sort parameters
     * @return page of {@link ChatSessionDTO} projected from JPA entities
     * @throws ResourceNotFoundException if no user with the given id exists
     */
    @Transactional(readOnly = true)
    public Page<ChatSessionDTO> getUserChats(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return chatSessionRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(ChatSessionDTO::fromEntity);
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Changes the role of the specified user and records the change in the
     * audit log.
     *
     * <p>Assigning the same role that the user already holds is a no-op from
     * a persistence standpoint but an audit entry is still written so that
     * attempted role changes are always traceable.
     *
     * @param userId    the UUID of the user whose role is to be changed
     * @param newRole   the new role to assign
     * @param actorId   the UUID of the uber_admin performing the change
     * @param ipAddress the client IP address from the HTTP request
     * @return the updated {@link UserDTO}
     * @throws ResourceNotFoundException if no user with the given id exists
     */
    @Transactional
    public UserDTO changeRole(UUID userId, UserRole newRole, UUID actorId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String oldValues = toJson(Map.of("role", user.getRole().name()));
        UserRole previousRole = user.getRole();

        user.setRole(newRole);
        User saved = userRepository.save(user);

        String newValues = toJson(Map.of("role", newRole.name()));

        auditService.log(actorId, "user.role_change", "User", userId,
                oldValues, newValues, ipAddress);

        log.info("Role changed for user {} from {} to {} by actor {}",
                userId, previousRole, newRole, actorId);

        return UserDTO.fromEntity(saved);
    }

    /**
     * Enables or disables the specified user account and records the change in
     * the audit log.
     *
     * <p>A disabled user cannot authenticate: the JWT filter will reject their
     * token because {@link com.tradeintel.auth.UserPrincipal#isEnabled()} returns
     * {@code false} for inactive accounts.
     *
     * @param userId    the UUID of the user to enable or disable
     * @param active    {@code true} to enable, {@code false} to disable
     * @param actorId   the UUID of the uber_admin performing the change
     * @param ipAddress the client IP address from the HTTP request
     * @return the updated {@link UserDTO}
     * @throws ResourceNotFoundException if no user with the given id exists
     */
    @Transactional
    public UserDTO toggleActive(UUID userId, boolean active, UUID actorId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String oldValues = toJson(Map.of("isActive", user.getIsActive()));

        user.setIsActive(active);
        User saved = userRepository.save(user);

        String newValues = toJson(Map.of("isActive", active));
        String action = active ? "user.activated" : "user.deactivated";

        auditService.log(actorId, action, "User", userId, oldValues, newValues, ipAddress);

        log.info("User {} isActive set to {} by actor {}", userId, active, actorId);

        return UserDTO.fromEntity(saved);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise audit snapshot: {}", e.getMessage());
            return "{}";
        }
    }
}
