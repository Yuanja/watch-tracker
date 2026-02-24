package com.tradeintel.normalize;

import com.tradeintel.common.entity.JargonEntry;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.normalize.dto.JargonCreateRequest;
import com.tradeintel.normalize.dto.JargonEntryDTO;
import com.tradeintel.normalize.dto.JargonUpdateRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing the jargon dictionary.
 *
 * <p>Handles admin CRUD operations, admin verification of LLM-discovered terms,
 * CSV generation for LLM extraction prompts, and the automated learning pipeline
 * that queues newly observed terms for admin review.</p>
 */
@Service
@Transactional
public class JargonService {

    private static final Logger log = LogManager.getLogger(JargonService.class);

    private final JargonRepository jargonRepository;

    public JargonService(JargonRepository jargonRepository) {
        this.jargonRepository = jargonRepository;
    }

    // -------------------------------------------------------------------------
    // List / search
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated list of jargon entries, optionally filtered by acronym.
     *
     * @param search   optional acronym substring to filter by (case-insensitive)
     * @param page     zero-based page index
     * @param size     page size
     * @return page of {@link JargonEntryDTO}
     */
    @Transactional(readOnly = true)
    public Page<JargonEntryDTO> list(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("acronym").ascending());

        Page<JargonEntry> entries;
        if (search != null && !search.isBlank()) {
            entries = jargonRepository.findByAcronymContainingIgnoreCase(search.trim(), pageable);
        } else {
            entries = jargonRepository.findAll(pageable);
        }

        log.debug("Listing jargon entries: search='{}', page={}, size={}, total={}",
                search, page, size, entries.getTotalElements());
        return entries.map(JargonEntryDTO::fromEntity);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new jargon dictionary entry.
     * Human-created entries are automatically verified.
     *
     * @param request the creation request
     * @return the created entry as a DTO
     * @throws IllegalArgumentException if an entry with the same acronym already exists
     */
    public JargonEntryDTO create(JargonCreateRequest request) {
        String acronym = request.getAcronym().trim();

        Optional<JargonEntry> existing = jargonRepository.findByAcronymIgnoreCase(acronym);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    "A jargon entry with acronym '" + acronym + "' already exists");
        }

        JargonEntry entry = new JargonEntry();
        entry.setAcronym(acronym);
        entry.setExpansion(request.getExpansion().trim());
        entry.setIndustry(request.getIndustry());
        entry.setContextExample(request.getContextExample());
        entry.setSource(request.getSource() != null ? request.getSource() : "human");
        entry.setConfidence(request.getConfidence() != null ? request.getConfidence() : 1.0);
        entry.setUsageCount(1);
        // Human-created entries are pre-verified; LLM entries start unverified.
        entry.setVerified("human".equals(entry.getSource()) || "seed".equals(entry.getSource()));

        JargonEntry saved = jargonRepository.save(entry);
        log.info("Created jargon entry: acronym='{}', id={}", saved.getAcronym(), saved.getId());
        return JargonEntryDTO.fromEntity(saved);
    }

    // -------------------------------------------------------------------------
    // Update / verify
    // -------------------------------------------------------------------------

    /**
     * Updates an existing jargon entry with any non-null fields from the request.
     *
     * @param id      the UUID of the entry to update
     * @param request partial update payload
     * @return the updated entry as a DTO
     * @throws ResourceNotFoundException if no entry with the given id exists
     */
    public JargonEntryDTO update(UUID id, JargonUpdateRequest request) {
        JargonEntry entry = jargonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JargonEntry", id));

        if (request.getAcronym() != null && !request.getAcronym().isBlank()) {
            entry.setAcronym(request.getAcronym().trim());
        }
        if (request.getExpansion() != null && !request.getExpansion().isBlank()) {
            entry.setExpansion(request.getExpansion().trim());
        }
        if (request.getIndustry() != null) {
            entry.setIndustry(request.getIndustry());
        }
        if (request.getContextExample() != null) {
            entry.setContextExample(request.getContextExample());
        }
        if (request.getConfidence() != null) {
            entry.setConfidence(request.getConfidence());
        }
        if (request.getVerified() != null) {
            entry.setVerified(request.getVerified());
        }

        JargonEntry saved = jargonRepository.save(entry);
        log.info("Updated jargon entry: id={}, acronym='{}'", saved.getId(), saved.getAcronym());
        return JargonEntryDTO.fromEntity(saved);
    }

    /**
     * Marks a jargon entry as admin-verified and boosts its confidence to 1.0.
     * After verification the entry is included in LLM extraction prompts.
     *
     * @param id the UUID of the entry to verify
     * @return the verified entry as a DTO
     * @throws ResourceNotFoundException if no entry with the given id exists
     */
    public JargonEntryDTO verify(UUID id) {
        JargonEntry entry = jargonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JargonEntry", id));

        entry.setVerified(true);
        entry.setConfidence(1.0);

        JargonEntry saved = jargonRepository.save(entry);
        log.info("Verified jargon entry: id={}, acronym='{}'", saved.getId(), saved.getAcronym());
        return JargonEntryDTO.fromEntity(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /**
     * Permanently removes a jargon entry.
     *
     * @param id the UUID of the entry to delete
     * @throws ResourceNotFoundException if no entry with the given id exists
     */
    public void delete(UUID id) {
        JargonEntry entry = jargonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JargonEntry", id));
        jargonRepository.delete(entry);
        log.info("Deleted jargon entry: id={}, acronym='{}'", id, entry.getAcronym());
    }

    // -------------------------------------------------------------------------
    // LLM prompt support
    // -------------------------------------------------------------------------

    /**
     * Returns a compact CSV representation of all verified jargon entries for
     * inclusion in the LLM extraction prompt.
     *
     * <p>Format: {@code ACRONYM,EXPANSION} per line, sorted alphabetically.</p>
     *
     * @return newline-delimited CSV string, or an empty string if no verified entries exist
     */
    @Transactional(readOnly = true)
    public String getVerifiedAsCSV() {
        List<JargonEntry> verified = jargonRepository.findByVerifiedTrueOrderByAcronymAsc();
        if (verified.isEmpty()) {
            return "";
        }
        return verified.stream()
                .map(e -> e.getAcronym() + "," + e.getExpansion())
                .collect(Collectors.joining("\n"));
    }

    // -------------------------------------------------------------------------
    // Auto-learning pipeline
    // -------------------------------------------------------------------------

    /**
     * Queues a list of newly observed terms from the LLM pipeline for admin review.
     *
     * <p>For each term:
     * <ul>
     *   <li>If it already exists (case-insensitive match), its {@code usageCount} is
     *       incremented and the entry is saved; no new entry is created.</li>
     *   <li>If it is genuinely new, an unverified entry with {@code source = "llm"} and
     *       {@code confidence = 0.5} is persisted for admin review.</li>
     * </ul>
     *
     * @param terms list of raw acronym strings observed in trade messages
     * @return list of newly created (unverified) {@link JargonEntryDTO}s — existing
     *         entries that were only incremented are excluded from the result
     */
    public List<JargonEntryDTO> learnNewTerms(List<String> terms) {
        List<JargonEntryDTO> created = new ArrayList<>();

        for (String term : terms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            String acronym = term.trim();
            Optional<JargonEntry> existing = jargonRepository.findByAcronymIgnoreCase(acronym);

            if (existing.isPresent()) {
                JargonEntry entry = existing.get();
                entry.setUsageCount(entry.getUsageCount() + 1);
                jargonRepository.save(entry);
                log.debug("Incremented usage count for existing jargon term: '{}'", acronym);
            } else {
                JargonEntry entry = new JargonEntry();
                entry.setAcronym(acronym);
                // Expansion unknown at learn-time; admin will fill it in during review.
                entry.setExpansion(acronym);
                entry.setSource("llm");
                entry.setConfidence(0.5);
                entry.setUsageCount(1);
                entry.setVerified(false);

                JargonEntry saved = jargonRepository.save(entry);
                log.info("Queued new jargon term for review: '{}'", acronym);
                created.add(JargonEntryDTO.fromEntity(saved));
            }
        }

        return created;
    }
}
