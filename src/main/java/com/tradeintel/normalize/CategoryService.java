package com.tradeintel.normalize;

import com.tradeintel.admin.AuditService;
import com.tradeintel.common.entity.Category;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.config.CacheConfig;
import com.tradeintel.normalize.dto.CategoryRequest;
import com.tradeintel.normalize.dto.CategoryResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for the admin-managed {@code Category} normalized value table.
 *
 * <p>Active category lists are cached in Caffeine (cache name {@code "categories"})
 * and evicted on any write operation so that the LLM extraction prompt always
 * reflects the current admin-managed state.
 *
 * <p>Deactivation is a soft-delete: the {@code is_active} flag is set to
 * {@code false} rather than physically removing the row, preserving referential
 * integrity with existing {@code listings} records.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Logger log = LogManager.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public CategoryService(CategoryRepository categoryRepository,
                           AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Lists all categories.
     *
     * @param activeOnly when {@code true} only active categories are returned,
     *                   ordered by sort_order; otherwise all categories are returned
     * @return list of category response DTOs
     */
    public List<CategoryResponse> list(boolean activeOnly) {
        List<Category> entities = activeOnly
                ? categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                : categoryRepository.findAll();
        return entities.stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns a comma-separated string of all active category names for injection
     * into LLM extraction prompts.
     *
     * <p>Result is cached under {@code "categories"} with the fixed key
     * {@code "allActiveNames"} so repeated prompt assembly does not hit the DB.
     *
     * @return CSV string, e.g. {@code "Pipe Fittings, Valves, Bearings"}
     */
    @Cacheable(value = CacheConfig.CACHE_CATEGORIES, key = "'allActiveNames'")
    public String getAllNamesAsCSV() {
        List<Category> active = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
        String csv = active.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));
        log.debug("Built categories CSV with {} entries", active.size());
        return csv;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Creates a new category.
     *
     * @param request the validated create request
     * @return the persisted category as a response DTO
     * @throws IllegalArgumentException if a category with the same name already exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        assertNameUnique(null, request.getName());

        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setIsActive(true);

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Created category id={} name='{}'", saved.getId(), saved.getName());
        auditService.log(null, "category.create", "Category", saved.getId(), null, null, null);
        return CategoryResponse.fromEntity(saved);
    }

    /**
     * Updates an existing category.
     *
     * @param id      the UUID of the category to update
     * @param request the validated update request
     * @return the updated category as a response DTO
     * @throws ResourceNotFoundException if no category with the given ID exists
     * @throws IllegalArgumentException  if the new name conflicts with another category
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        assertNameUnique(id, request.getName());

        category.setName(request.getName().trim());

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getParentId()));
            category.setParent(parent);
        } else {
            // Explicit null clears the parent (promotes to root)
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        log.info("Updated category id={} name='{}'", saved.getId(), saved.getName());
        auditService.log(null, "category.update", "Category", saved.getId(), null, null, null);
        return CategoryResponse.fromEntity(saved);
    }

    /**
     * Deactivates (soft-deletes) a category by setting {@code is_active = false}.
     * The row is retained so that existing listing references remain valid.
     *
     * @param id the UUID of the category to deactivate
     * @throws ResourceNotFoundException if no category with the given ID exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true)
    public void deactivate(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Deactivated category id={} name='{}'", id, category.getName());
        auditService.log(null, "category.deactivate", "Category", id, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Enforces unique category names across the table, excluding the row being updated.
     *
     * @param excludeId the ID of the entity being updated (null for creates)
     * @param name      the name to check
     * @throws IllegalArgumentException if the name is already taken by a different row
     */
    private void assertNameUnique(UUID excludeId, String name) {
        categoryRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                        "A category with the name '" + name.trim() + "' already exists");
            }
        });
    }
}
