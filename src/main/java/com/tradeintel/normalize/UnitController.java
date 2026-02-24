package com.tradeintel.normalize;

import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.normalize.dto.UnitRequest;
import com.tradeintel.normalize.dto.UnitResponse;
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
 * REST API for admin-managed {@code Unit} normalized values.
 *
 * <p>All endpoints require admin or uber_admin role via {@link AdminOnly}.
 *
 * <p>Base path: {@code /api/normalize/units}
 *
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/</td><td>List units (optionally filter by active)</td></tr>
 *   <tr><td>POST</td><td>/</td><td>Create a new unit (HTTP 201)</td></tr>
 *   <tr><td>PUT</td><td>/{id}</td><td>Update an existing unit (HTTP 200)</td></tr>
 *   <tr><td>DELETE</td><td>/{id}</td><td>Deactivate a unit (HTTP 204)</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/normalize/units")
@AdminOnly
public class UnitController {

    private static final Logger log = LogManager.getLogger(UnitController.class);

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    /**
     * Lists units.
     *
     * @param activeOnly when {@code true} (default) returns only active units;
     *                   pass {@code false} to include inactive units
     * @return HTTP 200 with a list of unit DTOs
     */
    @GetMapping
    public ResponseEntity<List<UnitResponse>> list(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {

        log.debug("GET /api/normalize/units activeOnly={}", activeOnly);
        List<UnitResponse> units = unitService.list(activeOnly);
        return ResponseEntity.ok(units);
    }

    /**
     * Creates a new unit.
     *
     * @param request validated request body
     * @return HTTP 201 Created with the new unit in the body and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<UnitResponse> create(@Valid @RequestBody UnitRequest request) {
        log.debug("POST /api/normalize/units name='{}'", request.getName());
        UnitResponse created = unitService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing unit.
     *
     * @param id      path variable — UUID of the unit to update
     * @param request validated request body with updated fields
     * @return HTTP 200 with the updated unit in the body
     */
    @PutMapping("/{id}")
    public ResponseEntity<UnitResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UnitRequest request) {

        log.debug("PUT /api/normalize/units/{}", id);
        UnitResponse updated = unitService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivates (soft-deletes) a unit.
     *
     * @param id path variable — UUID of the unit to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        log.debug("DELETE /api/normalize/units/{}", id);
        unitService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
