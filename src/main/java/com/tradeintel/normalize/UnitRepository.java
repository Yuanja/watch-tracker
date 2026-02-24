package com.tradeintel.normalize;

import com.tradeintel.common.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Unit} entities.
 *
 * <p>Provides standard CRUD operations plus domain-specific query methods
 * used by the admin normalize API and LLM prompt assembly.
 */
@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    /**
     * Returns all active units ordered alphabetically by name ascending.
     * Used when building the LLM extraction prompt and populating the admin UI.
     *
     * @return alphabetically ordered list of active units
     */
    List<Unit> findByIsActiveTrueOrderByNameAsc();

    /**
     * Looks up a unit by name, ignoring case.
     * Used during create/update to enforce the unique-name constraint
     * with a friendly error before the DB constraint fires.
     *
     * @param name the unit name to search for
     * @return an {@link Optional} containing the matching unit, or empty
     */
    Optional<Unit> findByNameIgnoreCase(String name);

    /**
     * Looks up a unit by abbreviation, ignoring case.
     * Used during create/update to enforce the unique-abbreviation constraint.
     *
     * @param abbreviation the abbreviation to search for
     * @return an {@link Optional} containing the matching unit, or empty
     */
    Optional<Unit> findByAbbreviationIgnoreCase(String abbreviation);
}
