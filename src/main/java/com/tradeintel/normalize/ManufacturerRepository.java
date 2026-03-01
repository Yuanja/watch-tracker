package com.tradeintel.normalize;

import com.tradeintel.common.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Manufacturer} entities.
 *
 * <p>Provides standard CRUD operations plus domain-specific query methods
 * used by the admin normalize API and LLM prompt assembly.
 */
@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {

    /**
     * Returns all active manufacturers ordered alphabetically by name ascending.
     * Used when building the LLM extraction prompt and populating the admin UI.
     *
     * @return alphabetically ordered list of active manufacturers
     */
    List<Manufacturer> findByIsActiveTrueOrderByNameAsc();

    /**
     * Looks up a manufacturer by name, ignoring case.
     * Used during create/update to enforce the unique-name constraint
     * with a friendly error before the DB constraint fires.
     *
     * @param name the manufacturer name to search for
     * @return an {@link Optional} containing the matching manufacturer, or empty
     */
    Optional<Manufacturer> findByNameIgnoreCase(String name);

    /**
     * Looks up a manufacturer by alias, ignoring case.
     * Searches the {@code aliases} text array for a matching entry so that
     * short forms like "AP" resolve to "Audemars Piguet".
     *
     * @param alias the alias to search for
     * @return an {@link Optional} containing the matching manufacturer, or empty
     */
    @Query(value = "SELECT m.* FROM manufacturers m WHERE m.is_active = true "
            + "AND EXISTS (SELECT 1 FROM unnest(m.aliases) a WHERE LOWER(a) = LOWER(:alias))",
            nativeQuery = true)
    Optional<Manufacturer> findByAliasIgnoreCase(@Param("alias") String alias);
}
