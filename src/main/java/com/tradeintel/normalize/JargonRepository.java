package com.tradeintel.normalize;

import com.tradeintel.common.entity.JargonEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link JargonEntry} entities.
 *
 * <p>Provides standard CRUD plus domain-specific query methods used by the
 * admin jargon management API and the LLM prompt assembly pipeline.</p>
 */
@Repository
public interface JargonRepository extends JpaRepository<JargonEntry, UUID> {

    /**
     * Returns all verified entries ordered alphabetically by acronym.
     * Used when building the LLM extraction prompt to expand known jargon.
     *
     * @return ordered list of verified jargon entries
     */
    List<JargonEntry> findByVerifiedTrueOrderByAcronymAsc();

    /**
     * Looks up an entry by acronym, ignoring case.
     * Used during create/update and for de-duplication in the learn pipeline.
     *
     * @param acronym the acronym to search for
     * @return an {@link Optional} containing the matching entry, or empty
     */
    Optional<JargonEntry> findByAcronymIgnoreCase(String acronym);

    /**
     * Returns all unverified entries ordered by creation time descending,
     * forming the admin review queue for LLM-discovered terms.
     *
     * @return unverified entries newest-first
     */
    List<JargonEntry> findByVerifiedFalseOrderByCreatedAtDesc();

    /**
     * Paginated search by acronym prefix, case-insensitive.
     * Used by the admin list endpoint when a search query is provided.
     *
     * @param acronym  the acronym prefix to search (use {@code %value%} for contains)
     * @param pageable pagination and sorting parameters
     * @return page of matching entries
     */
    Page<JargonEntry> findByAcronymContainingIgnoreCase(String acronym, Pageable pageable);
}
