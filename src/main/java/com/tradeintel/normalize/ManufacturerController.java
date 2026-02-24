package com.tradeintel.normalize;

import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.normalize.dto.ManufacturerRequest;
import com.tradeintel.normalize.dto.ManufacturerResponse;
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
 * REST API for admin-managed {@code Manufacturer} normalized values.
 *
 * <p>All endpoints require admin or uber_admin role via {@link AdminOnly}.
 *
 * <p>Base path: {@code /api/normalize/manufacturers}
 *
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/</td><td>List manufacturers (optionally filter by active)</td></tr>
 *   <tr><td>POST</td><td>/</td><td>Create a new manufacturer (HTTP 201)</td></tr>
 *   <tr><td>PUT</td><td>/{id}</td><td>Update an existing manufacturer (HTTP 200)</td></tr>
 *   <tr><td>DELETE</td><td>/{id}</td><td>Deactivate a manufacturer (HTTP 204)</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/normalize/manufacturers")
@AdminOnly
public class ManufacturerController {

    private static final Logger log = LogManager.getLogger(ManufacturerController.class);

    private final ManufacturerService manufacturerService;

    public ManufacturerController(ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    /**
     * Lists manufacturers.
     *
     * @param activeOnly when {@code true} (default) returns only active manufacturers;
     *                   pass {@code false} to include inactive manufacturers
     * @return HTTP 200 with a list of manufacturer DTOs
     */
    @GetMapping
    public ResponseEntity<List<ManufacturerResponse>> list(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {

        log.debug("GET /api/normalize/manufacturers activeOnly={}", activeOnly);
        List<ManufacturerResponse> manufacturers = manufacturerService.list(activeOnly);
        return ResponseEntity.ok(manufacturers);
    }

    /**
     * Creates a new manufacturer.
     *
     * @param request validated request body
     * @return HTTP 201 Created with the new manufacturer in the body and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<ManufacturerResponse> create(
            @Valid @RequestBody ManufacturerRequest request) {

        log.debug("POST /api/normalize/manufacturers name='{}'", request.getName());
        ManufacturerResponse created = manufacturerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing manufacturer.
     *
     * @param id      path variable — UUID of the manufacturer to update
     * @param request validated request body with updated fields
     * @return HTTP 200 with the updated manufacturer in the body
     */
    @PutMapping("/{id}")
    public ResponseEntity<ManufacturerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ManufacturerRequest request) {

        log.debug("PUT /api/normalize/manufacturers/{}", id);
        ManufacturerResponse updated = manufacturerService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivates (soft-deletes) a manufacturer.
     *
     * @param id path variable — UUID of the manufacturer to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        log.debug("DELETE /api/normalize/manufacturers/{}", id);
        manufacturerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
