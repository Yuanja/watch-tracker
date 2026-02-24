package com.tradeintel.normalize;

import com.tradeintel.common.entity.Condition;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.config.CacheConfig;
import com.tradeintel.normalize.dto.ConditionRequest;
import com.tradeintel.normalize.dto.ConditionResponse;
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
 * Business logic for the admin-managed {@code Condition} normalized value table.
 *
 * <p>Active condition lists are cached in Caffeine (cache name {@code "conditions"})
 * and evicted on any write so that the LLM extraction prompt reflects current state.
 *
 * <p>Deactivation is a soft-delete: the {@code is_active} flag is set to
 * {@code false}, preserving referential integrity with existing {@code listings}
 * records.
 */
@Service
@Transactional(readOnly = true)
public class ConditionService {

    private static final Logger log = LogManager.getLogger(ConditionService.class);

    private final ConditionRepository conditionRepository;

    public ConditionService(ConditionRepository conditionRepository) {
        this.conditionRepository = conditionRepository;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Lists all conditions.
     *
     * @param activeOnly when {@code true} only active conditions are returned,
     *                   ordered by sort_order; otherwise all conditions are returned
     * @return list of condition response DTOs
     */
    public List<ConditionResponse> list(boolean activeOnly) {
        List<Condition> entities = activeOnly
                ? conditionRepository.findByIsActiveTrueOrderBySortOrderAsc()
                : conditionRepository.findAll();
        return entities.stream()
                .map(ConditionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns a comma-separated string of all active condition names for injection
     * into LLM extraction prompts.
     *
     * <p>Each condition is represented as {@code "name (abbreviation)"} when an
     * abbreviation is present, or just the name otherwise.
     * Result is cached under {@code "conditions"} with the fixed key {@code "allActiveNames"}.
     *
     * @return CSV string ready to embed in a prompt
     */
    @Cacheable(value = CacheConfig.CACHE_CONDITIONS, key = "'allActiveNames'")
    public String getAllNamesAsCSV() {
        List<Condition> active = conditionRepository.findByIsActiveTrueOrderBySortOrderAsc();
        String csv = active.stream()
                .map(c -> {
                    if (c.getAbbreviation() != null && !c.getAbbreviation().isBlank()) {
                        return c.getName() + " (" + c.getAbbreviation() + ")";
                    }
                    return c.getName();
                })
                .collect(Collectors.joining(", "));
        log.debug("Built conditions CSV with {} entries", active.size());
        return csv;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Creates a new condition.
     *
     * @param request the validated create request
     * @return the persisted condition as a response DTO
     * @throws IllegalArgumentException if a condition with the same name already exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CONDITIONS, allEntries = true)
    public ConditionResponse create(ConditionRequest request) {
        assertNameUnique(null, request.getName());

        Condition condition = new Condition();
        condition.setName(request.getName().trim());
        condition.setAbbreviation(request.getAbbreviation());
        condition.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        condition.setIsActive(true);

        Condition saved = conditionRepository.save(condition);
        log.info("Created condition id={} name='{}'", saved.getId(), saved.getName());
        return ConditionResponse.fromEntity(saved);
    }

    /**
     * Updates an existing condition.
     *
     * @param id      the UUID of the condition to update
     * @param request the validated update request
     * @return the updated condition as a response DTO
     * @throws ResourceNotFoundException if no condition with the given ID exists
     * @throws IllegalArgumentException  if the new name conflicts with another condition
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CONDITIONS, allEntries = true)
    public ConditionResponse update(UUID id, ConditionRequest request) {
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition", id));

        assertNameUnique(id, request.getName());

        condition.setName(request.getName().trim());
        condition.setAbbreviation(request.getAbbreviation());

        if (request.getSortOrder() != null) {
            condition.setSortOrder(request.getSortOrder());
        }

        Condition saved = conditionRepository.save(condition);
        log.info("Updated condition id={} name='{}'", saved.getId(), saved.getName());
        return ConditionResponse.fromEntity(saved);
    }

    /**
     * Deactivates (soft-deletes) a condition by setting {@code is_active = false}.
     *
     * @param id the UUID of the condition to deactivate
     * @throws ResourceNotFoundException if no condition with the given ID exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_CONDITIONS, allEntries = true)
    public void deactivate(UUID id) {
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition", id));
        condition.setIsActive(false);
        conditionRepository.save(condition);
        log.info("Deactivated condition id={} name='{}'", id, condition.getName());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void assertNameUnique(UUID excludeId, String name) {
        conditionRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                        "A condition with the name '" + name.trim() + "' already exists");
            }
        });
    }
}
