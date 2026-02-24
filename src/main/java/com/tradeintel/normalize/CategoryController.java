package com.tradeintel.normalize;

import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.normalize.dto.CategoryRequest;
import com.tradeintel.normalize.dto.CategoryResponse;
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
 * REST API for admin-managed {@code Category} normalized values.
 *
 * <p>All endpoints require admin or uber_admin role via {@link AdminOnly}.
 *
 * <p>Base path: {@code /api/normalize/categories}
 *
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>GET</td><td>/</td><td>List categories (optionally filter by active)</td></tr>
 *   <tr><td>POST</td><td>/</td><td>Create a new category (HTTP 201)</td></tr>
 *   <tr><td>PUT</td><td>/{id}</td><td>Update an existing category (HTTP 200)</td></tr>
 *   <tr><td>DELETE</td><td>/{id}</td><td>Deactivate a category (HTTP 204)</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/normalize/categories")
@AdminOnly
public class CategoryController {

    private static final Logger log = LogManager.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Lists categories.
     *
     * @param activeOnly when {@code true} (default) returns only active categories;
     *                   pass {@code false} to include inactive categories
     * @return HTTP 200 with a list of category DTOs
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {

        log.debug("GET /api/normalize/categories activeOnly={}", activeOnly);
        List<CategoryResponse> categories = categoryService.list(activeOnly);
        return ResponseEntity.ok(categories);
    }

    /**
     * Creates a new category.
     *
     * @param request validated request body
     * @return HTTP 201 Created with the new category in the body and a {@code Location} header
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        log.debug("POST /api/normalize/categories name='{}'", request.getName());
        CategoryResponse created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing category.
     *
     * @param id      path variable — UUID of the category to update
     * @param request validated request body with updated fields
     * @return HTTP 200 with the updated category in the body
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {

        log.debug("PUT /api/normalize/categories/{}", id);
        CategoryResponse updated = categoryService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivates (soft-deletes) a category.
     *
     * @param id path variable — UUID of the category to deactivate
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        log.debug("DELETE /api/normalize/categories/{}", id);
        categoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
