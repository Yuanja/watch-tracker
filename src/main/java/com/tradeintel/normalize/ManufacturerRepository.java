package com.tradeintel.normalize;

import com.tradeintel.common.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
