package com.tradeintel.review;

import com.tradeintel.auth.UserPrincipal;
import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.review.dto.ResolutionRequest;
import jakarta.validation.Valid;
import com.tradeintel.review.dto.ReviewItemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
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
 * REST controller for the admin review queue.
 *
 * <p>All endpoints require admin or uber_admin role (enforced by the
 * {@link AdminOnly} meta-annotation at the class level and Spring Security's
 * URL-based authorization for {@code /api/review/**}).</p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>{@code GET /api/review} — list pending review items (paginated)</li>
 *   <li>{@code POST /api/review/{id}/resolve} — resolve a review item with corrections</li>
 *   <li>{@code POST /api/review/{id}/skip} — skip a review item</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/review")
@AdminOnly
public class ReviewController {

    private static final Logger log = LogManager.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Lists pending review items with pagination.
     *
     * @param page zero-based page index (default 0)
     * @param size page size (default 20)
     * @return page of pending review items
     */
    @GetMapping
    public ResponseEntity<Page<ReviewItemDTO>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/review page={} size={}", page, size);
        Page<ReviewItemDTO> items = reviewService.listPending(page, size);
        return ResponseEntity.ok(items);
    }

    /**
     * Resolves a review item by applying admin corrections to the associated listing.
     * The listing is promoted to active status.
     *
     * @param id        the UUID of the review queue item
     * @param request   the admin's corrections
     * @param principal the authenticated admin user
     * @return the resolved review item
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ReviewItemDTO> resolve(
            @PathVariable UUID id,
            @Valid @RequestBody ResolutionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("POST /api/review/{}/resolve by user {}", id, principal.getUserId());
        ReviewItemDTO result = reviewService.resolve(id, request, principal.getUser());
        return ResponseEntity.ok(result);
    }

    /**
     * Skips a review item without making any changes to the associated listing.
     *
     * @param id        the UUID of the review queue item
     * @param principal the authenticated admin user
     * @return the skipped review item
     */
    @PostMapping("/{id}/skip")
    public ResponseEntity<ReviewItemDTO> skip(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("POST /api/review/{}/skip by user {}", id, principal.getUserId());
        ReviewItemDTO result = reviewService.skip(id, principal.getUser());
        return ResponseEntity.ok(result);
    }
}
