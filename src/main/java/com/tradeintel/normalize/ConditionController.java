package com.tradeintel.normalize;

import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.normalize.dto.ConditionRequest;
import com.tradeintel.normalize.dto.ConditionResponse;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST API for admin-managed {@code Condition} normalized values.
 *
 * <p>All endpoints require admin or uber_admin role via {@link AdminOnly}.
 *
 * <p>Base path: {@code /api/normalize/conditions}
 *
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/</td><td>List conditions (optionally filter by active)</td></tr>
 *   <tr><td>POST</td><td>/</td><td>Create a new condition (HTTP 201)</td></tr>
 *   <tr><td>PUT</td><td>/{id}</td><td>Update an existing condition (HTTP 200)</td></tr>
 *   <tr><td>DELETE</td><td>/{id}</td><td>Deactivate a condition (HTTP 204)</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/normalize/conditions")
@AdminOnly
public class ConditionController {

    private static final Logger log = LogManager.getLogger(ConditionController.class);

    private final ConditionService conditionService;

    public ConditionController(ConditionService conditionService) {
        this.conditionService = conditionService;
    }

    /**
     * Lists conditions.
     *
     * @param activeOnly when {@code true} (default) returns only active conditions;
     *                   pass {@code false} to include inactive conditions
     * @return HTTP 200 with a list of condition DTOs
     */
    @GetMapping
    public ResponseEntity<List<ConditionResponse>> list(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {

        log.debug("GET /api/normalize/conditions activeOnly={}", activeOnly);
        List<ConditionResponse> conditions = conditionService.list(activeOnly);
        return ResponseEntity.ok(conditions);
    }

    /**
     * Creates a new condition.
     *
     * @param request validated request body
     * @return HTTP 201 Created with the new condition in the body and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<ConditionResponse> create(@Valid @RequestBody ConditionRequest request) {
        log.debug("POST /api/normalize/conditions name='{}'", request.getName());
        ConditionResponse created = conditionService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing condition.
     *
     * @param id      path variable — UUID of the condition to update
     * @param request validated request body with updated fields
     * @return HTTP 200 with the updated condition in the body
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConditionResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ConditionRequest request) {

        log.debug("PUT /api/normalize/conditions/{}", id);
        ConditionResponse updated = conditionService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivates (soft-deletes) a condition.
     *
     * @param id path variable — UUID of the condition to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        log.debug("DELETE /api/normalize/conditions/{}", id);
        conditionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
