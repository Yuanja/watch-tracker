package com.tradeintel.normalize;

import com.tradeintel.common.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Condition} entities.
 *
 * <p>Provides standard CRUD operations plus domain-specific query methods
 * used by the admin normalize API and LLM prompt assembly.
 */
@Repository
public interface ConditionRepository extends JpaRepository<Condition, UUID> {

    /**
     * Returns all active conditions ordered by their {@code sort_order} column ascending.
     * Used when building the LLM extraction prompt and populating the admin UI.
     *
     * @return ordered list of active conditions
     */
    List<Condition> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * Looks up a condition by name, ignoring case.
     * Used during create/update to enforce the unique-name constraint
     * with a friendly error before the DB constraint fires.
     *
     * @param name the condition name to search for
     * @return an {@link Optional} containing the matching condition, or empty
     */
    Optional<Condition> findByNameIgnoreCase(String name);

    /**
     * Looks up a condition by its abbreviation, ignoring case.
     * Used as a fallback during extraction when the LLM outputs a short form
     * like "new", "BNIB", or "pre-owned" instead of the full condition name.
     *
     * @param abbreviation the abbreviation to search for
     * @return an {@link Optional} containing the matching condition, or empty
     */
    Optional<Condition> findByAbbreviationIgnoreCase(String abbreviation);
}
