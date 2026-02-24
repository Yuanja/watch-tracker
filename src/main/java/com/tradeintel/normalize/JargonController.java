package com.tradeintel.normalize;

import com.tradeintel.common.security.AdminOnly;
import com.tradeintel.normalize.dto.JargonCreateRequest;
import com.tradeintel.normalize.dto.JargonEntryDTO;
import com.tradeintel.normalize.dto.JargonUpdateRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the jargon dictionary admin API.
 *
 * <p>All endpoints require at minimum the {@code admin} role ({@link AdminOnly}).
 * The resource path is {@code /api/jargon}.</p>
 *
 * <p>The jargon dictionary drives two features:
 * <ul>
 *   <li><b>LLM prompt injection</b> — verified entries are serialised to CSV and
 *       inserted into the extraction prompt so GPT-4o-mini can map trade
 *       abbreviations to their canonical expansions.</li>
 *   <li><b>Auto-learning queue</b> — the pipeline calls {@code POST /api/jargon/learn}
 *       to queue newly observed terms for admin review.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/jargon")
@AdminOnly
public class JargonController {

    private static final Logger log = LogManager.getLogger(JargonController.class);

    private final JargonService jargonService;

    public JargonController(JargonService jargonService) {
        this.jargonService = jargonService;
    }

    // -------------------------------------------------------------------------
    // GET /api/jargon  — paginated list with optional search
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated list of jargon entries, optionally filtered by acronym.
     *
     * @param search optional acronym substring filter
     * @param page   zero-based page index (default 0)
     * @param size   page size (default 50)
     * @return page of {@link JargonEntryDTO}
     */
    @GetMapping
    public ResponseEntity<Page<JargonEntryDTO>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("GET /api/jargon search='{}' page={} size={}", search, page, size);
        Page<JargonEntryDTO> result = jargonService.list(search, page, size);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // POST /api/jargon  — create
    // -------------------------------------------------------------------------

    /**
     * Creates a new jargon entry.
     *
     * @param request creation payload (acronym + expansion are required)
     * @return 201 Created with the new entry and {@code Location} header
     */
    @PostMapping
    public ResponseEntity<JargonEntryDTO> create(@Valid @RequestBody JargonCreateRequest request) {
        log.info("POST /api/jargon acronym='{}'", request.getAcronym());
        JargonEntryDTO created = jargonService.create(request);
        URI location = URI.create("/api/jargon/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    // -------------------------------------------------------------------------
    // PUT /api/jargon/{id}  — update / verify
    // -------------------------------------------------------------------------

    /**
     * Updates fields on an existing jargon entry.
     * Set {@code verified: true} in the request body to approve the entry for
     * inclusion in LLM extraction prompts.
     *
     * @param id      the entry UUID
     * @param request partial update payload
     * @return the updated entry
     */
    @PutMapping("/{id}")
    public ResponseEntity<JargonEntryDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody JargonUpdateRequest request) {

        log.info("PUT /api/jargon/{}", id);
        JargonEntryDTO updated = jargonService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------------------------
    // POST /api/jargon/{id}/verify  — dedicated verify action
    // -------------------------------------------------------------------------

    /**
     * Convenience endpoint to verify a single jargon entry.
     * Equivalent to {@code PUT /{id}} with {@code {"verified": true, "confidence": 1.0}}.
     *
     * @param id the entry UUID
     * @return the verified entry
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<JargonEntryDTO> verify(@PathVariable UUID id) {
        log.info("POST /api/jargon/{}/verify", id);
        JargonEntryDTO verified = jargonService.verify(id);
        return ResponseEntity.ok(verified);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/jargon/{id}  — remove entry
    // -------------------------------------------------------------------------

    /**
     * Permanently removes a jargon entry.
     *
     * @param id the entry UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("DELETE /api/jargon/{}", id);
        jargonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // GET /api/jargon/unverified  — review queue
    // -------------------------------------------------------------------------

    /**
     * Returns all unverified (LLM-discovered) entries awaiting admin review,
     * ordered newest-first.
     *
     * @return list of unverified entries
     */
    @GetMapping("/unverified")
    public ResponseEntity<List<JargonEntryDTO>> listUnverified() {
        log.debug("GET /api/jargon/unverified");
        List<JargonEntryDTO> unverified = jargonService.list(null, 0, Integer.MAX_VALUE)
                .filter(dto -> Boolean.FALSE.equals(dto.getVerified()))
                .toList();
        return ResponseEntity.ok(unverified);
    }

    // -------------------------------------------------------------------------
    // POST /api/jargon/learn  — internal learn pipeline
    // -------------------------------------------------------------------------

    /**
     * Queues a batch of newly observed terms from the LLM pipeline for admin review.
     * Existing terms have their usage counts incremented; unknown terms are persisted
     * as unverified entries.
     *
     * @param terms list of raw acronym strings
     * @return list of newly created (unverified) entries
     */
    @PostMapping("/learn")
    public ResponseEntity<List<JargonEntryDTO>> learnNewTerms(
            @RequestBody List<String> terms) {

        log.info("POST /api/jargon/learn terms={}", terms.size());
        List<JargonEntryDTO> created = jargonService.learnNewTerms(terms);
        return ResponseEntity.ok(created);
    }
}
