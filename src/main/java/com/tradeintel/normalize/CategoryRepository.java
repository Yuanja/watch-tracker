package com.tradeintel.normalize;

import com.tradeintel.common.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Category} entities.
 *
 * <p>Provides standard CRUD operations plus domain-specific query methods
 * used by the admin normalize API and LLM prompt assembly.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Returns all active categories ordered by their {@code sort_order} column ascending.
     * Used when building the LLM extraction prompt and populating the admin UI.
     *
     * @return ordered list of active categories
     */
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * Looks up a category by name, ignoring case.
     * Used during create/update to enforce the unique-name constraint
     * with a friendly error before the DB constraint fires.
     *
     * @param name the category name to search for
     * @return an {@link Optional} containing the matching category, or empty
     */
    Optional<Category> findByNameIgnoreCase(String name);
}
