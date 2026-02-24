package com.tradeintel.admin;

import com.tradeintel.admin.dto.AuditLogEntryDTO;
import com.tradeintel.admin.dto.ChangeRoleRequest;
import com.tradeintel.admin.dto.ChatSessionDTO;
import com.tradeintel.admin.dto.CreateGroupRequest;
import com.tradeintel.admin.dto.ToggleActiveRequest;
import com.tradeintel.admin.dto.UpdateGroupRequest;
import com.tradeintel.admin.dto.UserCostSummaryDTO;
import com.tradeintel.admin.dto.UserDTO;
import com.tradeintel.admin.dto.WhatsappGroupDTO;
import com.tradeintel.archive.WhatsappGroupRepository;
import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.common.entity.WhatsappGroup;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.common.security.UberAdminOnly;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing uber_admin platform management endpoints under
 * {@code /api/admin}.
 *
 * <p>Every method is annotated with {@link UberAdminOnly}, which is a
 * meta-annotation that expands to
 * {@code @PreAuthorize("hasRole('UBER_ADMIN')")}. Spring Security evaluates
 * this before any method body executes, so unauthenticated or insufficiently
 * privileged callers receive a 403 before any service logic runs.
 *
 * <p>The client IP address is extracted from the incoming
 * {@link HttpServletRequest} and forwarded to service methods that write
 * audit log entries.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LogManager.getLogger(AdminController.class);

    private final UserManagementService userManagementService;
    private final CostReportService costReportService;
    private final AuditService auditService;
    private final WhatsappGroupRepository groupRepository;

    public AdminController(UserManagementService userManagementService,
                           CostReportService costReportService,
                           AuditService auditService,
                           WhatsappGroupRepository groupRepository) {
        this.userManagementService = userManagementService;
        this.costReportService = costReportService;
        this.auditService = auditService;
        this.groupRepository = groupRepository;
    }

    // =========================================================================
    // User management
    // =========================================================================

    /**
     * Returns a paginated list of all platform users.
     *
     * <p>GET /api/admin/users?page=0&size=20&sort=createdAt,desc
     *
     * @param page  zero-based page index (default 0)
     * @param size  page size (default 20)
     * @return 200 with a {@link Page} of {@link UserDTO}
     */
    @UberAdminOnly
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserDTO> result = userManagementService.listUsers(pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Changes the role of the specified user.
     *
     * <p>PUT /api/admin/users/{id}/role
     * <p>Body: {@code {"role": "admin"}}
     *
     * @param id        the UUID of the target user
     * @param request   validated request body containing the new role
     * @param principal the authenticated uber_admin
     * @param req       HTTP request used to extract the client IP
     * @return 200 with the updated {@link UserDTO}
     */
    @UberAdminOnly
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest req) {

        String ip = extractIp(req);
        log.info("Role change requested for user {} to {} by {}", id, request.role(), principal.getUserId());

        UserDTO updated = userManagementService.changeRole(id, request.role(), principal.getUserId(), ip);
        return ResponseEntity.ok(updated);
    }

    /**
     * Enables or disables the specified user account.
     *
     * <p>PUT /api/admin/users/{id}/active
     * <p>Body: {@code {"active": false}}
     *
     * @param id        the UUID of the target user
     * @param request   validated request body containing the desired active state
     * @param principal the authenticated uber_admin
     * @param req       HTTP request used to extract the client IP
     * @return 200 with the updated {@link UserDTO}
     */
    @UberAdminOnly
    @PutMapping("/users/{id}/active")
    public ResponseEntity<UserDTO> toggleActive(
            @PathVariable UUID id,
            @Valid @RequestBody ToggleActiveRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest req) {

        String ip = extractIp(req);
        log.info("Active toggle requested for user {} to {} by {}", id, request.active(), principal.getUserId());

        UserDTO updated = userManagementService.toggleActive(id, request.active(), principal.getUserId(), ip);
        return ResponseEntity.ok(updated);
    }

    /**
     * Returns a paginated list of chat sessions belonging to the specified user.
     *
     * <p>GET /api/admin/users/{id}/chats?page=0&size=20
     *
     * @param id   the UUID of the target user
     * @param page zero-based page index (default 0)
     * @param size page size (default 20)
     * @return 200 with a {@link Page} of {@link ChatSessionDTO}
     */
    @UberAdminOnly
    @GetMapping("/users/{id}/chats")
    public ResponseEntity<Page<ChatSessionDTO>> getUserChats(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatSessionDTO> sessions = userManagementService.getUserChats(id, pageable);
        return ResponseEntity.ok(sessions);
    }

    // =========================================================================
    // Cost reports
    // =========================================================================

    /**
     * Returns a cost breakdown aggregated per user for the given date range.
     * Results are sorted by total cost descending (highest spenders first).
     *
     * <p>GET /api/admin/costs?startDate=2026-01-01&endDate=2026-01-31
     *
     * @param startDate reporting period start (ISO date, inclusive; defaults to 30 days ago)
     * @param endDate   reporting period end   (ISO date, inclusive; defaults to today)
     * @return 200 with a list of {@link UserCostSummaryDTO}
     */
    @UberAdminOnly
    @GetMapping("/costs")
    public ResponseEntity<List<UserCostSummaryDTO>> getAllUsersCosts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusDays(30);

        List<UserCostSummaryDTO> costs = costReportService.getAllUsersCosts(resolvedStart, resolvedEnd);
        return ResponseEntity.ok(costs);
    }

    /**
     * Exports all user costs within the given date range as a CSV file download.
     *
     * <p>GET /api/admin/costs/export?startDate=2026-01-01&endDate=2026-01-31
     *
     * <p>Returns {@code Content-Type: text/csv} and
     * {@code Content-Disposition: attachment; filename="costs-<start>-<end>.csv"}.
     *
     * @param startDate reporting period start (ISO date, inclusive; defaults to 30 days ago)
     * @param endDate   reporting period end   (ISO date, inclusive; defaults to today)
     * @return 200 with CSV body as a byte array
     */
    @UberAdminOnly
    @GetMapping("/costs/export")
    public ResponseEntity<byte[]> exportCosts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusDays(30);

        String csv = costReportService.exportCostsCsv(resolvedStart, resolvedEnd);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        String filename = "costs-" + resolvedStart + "-" + resolvedEnd + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);

        log.info("Exporting costs CSV: {} to {}, {} bytes", resolvedStart, resolvedEnd, bytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }

    // =========================================================================
    // Audit log
    // =========================================================================

    /**
     * Returns a paginated audit log, optionally filtered by the actor who
     * performed the actions.
     *
     * <p>GET /api/admin/audit?page=0&size=50&actorId=<uuid>
     *
     * @param page    zero-based page index (default 0)
     * @param size    page size (default 50)
     * @param actorId optional UUID to filter entries to a single actor
     * @return 200 with a {@link Page} of {@link AuditLogEntryDTO}
     */
    @UberAdminOnly
    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLogEntryDTO>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID actorId) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogEntryDTO> entries = auditService.getAuditLog(actorId, pageable);
        return ResponseEntity.ok(entries);
    }

    // =========================================================================
    // WhatsApp group management
    // =========================================================================

    /**
     * Returns all monitored WhatsApp groups (both active and inactive).
     *
     * <p>GET /api/admin/groups
     *
     * @return 200 with a list of {@link WhatsappGroupDTO}
     */
    @UberAdminOnly
    @GetMapping("/groups")
    public ResponseEntity<List<WhatsappGroupDTO>> listGroups() {
        List<WhatsappGroupDTO> groups = groupRepository.findAll(Sort.by("groupName").ascending())
                .stream()
                .map(WhatsappGroupDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(groups);
    }

    /**
     * Registers a new WhatsApp group for monitoring.
     *
     * <p>POST /api/admin/groups
     *
     * @param request   validated create request containing the Whapi group id and name
     * @param principal the authenticated uber_admin
     * @param req       HTTP request used to extract the client IP for audit logging
     * @return 200 with the created {@link WhatsappGroupDTO}
     */
    @UberAdminOnly
    @PostMapping("/groups")
    public ResponseEntity<WhatsappGroupDTO> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest req) {

        WhatsappGroup group = new WhatsappGroup();
        group.setWhapiGroupId(request.whapiGroupId());
        group.setGroupName(request.groupName());
        group.setDescription(request.description());
        group.setAvatarUrl(request.avatarUrl());
        group.setIsActive(true);

        WhatsappGroup saved = groupRepository.save(group);

        auditService.log(
                principal.getUserId(),
                "group.created",
                "WhatsappGroup",
                saved.getId(),
                null,
                toGroupJson(saved),
                extractIp(req)
        );

        log.info("WhatsApp group created: {} ({}) by {}", saved.getGroupName(), saved.getId(), principal.getUserId());

        return ResponseEntity.ok(WhatsappGroupDTO.fromEntity(saved));
    }

    /**
     * Updates the mutable display settings of a monitored WhatsApp group.
     * The Whapi group identifier is immutable and cannot be changed here.
     *
     * <p>PUT /api/admin/groups/{id}
     *
     * @param id        the internal UUID of the group to update
     * @param request   validated update request
     * @param principal the authenticated uber_admin
     * @param req       HTTP request used to extract the client IP for audit logging
     * @return 200 with the updated {@link WhatsappGroupDTO}
     */
    @UberAdminOnly
    @PutMapping("/groups/{id}")
    public ResponseEntity<WhatsappGroupDTO> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest req) {

        WhatsappGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp group not found: " + id));

        String oldValues = toGroupJson(group);

        group.setGroupName(request.groupName());
        group.setDescription(request.description());
        group.setAvatarUrl(request.avatarUrl());

        WhatsappGroup saved = groupRepository.save(group);

        auditService.log(
                principal.getUserId(),
                "group.updated",
                "WhatsappGroup",
                id,
                oldValues,
                toGroupJson(saved),
                extractIp(req)
        );

        log.info("WhatsApp group updated: {} ({}) by {}", saved.getGroupName(), id, principal.getUserId());

        return ResponseEntity.ok(WhatsappGroupDTO.fromEntity(saved));
    }

    /**
     * Deactivates a monitored WhatsApp group, stopping further message ingestion.
     * The group record is retained for historical replay; no data is deleted.
     *
     * <p>DELETE /api/admin/groups/{id}
     *
     * @param id        the internal UUID of the group to deactivate
     * @param principal the authenticated uber_admin
     * @param req       HTTP request used to extract the client IP for audit logging
     * @return 204 No Content on success
     */
    @UberAdminOnly
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deactivateGroup(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest req) {

        WhatsappGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp group not found: " + id));

        String oldValues = toGroupJson(group);

        group.setIsActive(false);
        groupRepository.save(group);

        auditService.log(
                principal.getUserId(),
                "group.deactivated",
                "WhatsappGroup",
                id,
                oldValues,
                "{\"isActive\":false}",
                extractIp(req)
        );

        log.info("WhatsApp group deactivated: {} ({}) by {}", group.getGroupName(), id, principal.getUserId());

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Extracts the most specific client IP from the request, preferring the
     * {@code X-Forwarded-For} header (set by reverse proxies) over the raw
     * remote address.
     *
     * @param request the incoming HTTP servlet request
     * @return the client IP address string, or the direct remote address if no
     *         forwarded header is present
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a comma-separated chain; the first is the client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Produces a minimal JSON representation of a {@link WhatsappGroup} for
     * audit log snapshots. Intentionally hand-rolled to avoid a Jackson
     * dependency on entities without {@code @JsonSerialize} annotations.
     *
     * @param group the entity to serialise
     * @return a JSON string suitable for storing in the audit log
     */
    private String toGroupJson(WhatsappGroup group) {
        return String.format(
                "{\"groupName\":%s,\"description\":%s,\"avatarUrl\":%s,\"isActive\":%b}",
                jsonString(group.getGroupName()),
                jsonString(group.getDescription()),
                jsonString(group.getAvatarUrl()),
                Boolean.TRUE.equals(group.getIsActive())
        );
    }

    /**
     * Wraps a string in JSON double-quotes, escaping internal double-quotes and
     * backslashes. Returns {@code null} (JSON null literal) for null input.
     *
     * @param value the raw string value
     * @return a JSON-quoted string or {@code "null"}
     */
    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
