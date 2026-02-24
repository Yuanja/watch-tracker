package com.tradeintel.normalize;

import com.tradeintel.common.entity.Unit;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.config.CacheConfig;
import com.tradeintel.normalize.dto.UnitRequest;
import com.tradeintel.normalize.dto.UnitResponse;
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
 * Business logic for the admin-managed {@code Unit} normalized value table.
 *
 * <p>Active unit lists are cached in Caffeine (cache name {@code "units"})
 * and evicted on any write so that the LLM extraction prompt reflects current state.
 *
 * <p>Deactivation is a soft-delete: the {@code is_active} flag is set to
 * {@code false}, preserving referential integrity with existing {@code listings}
 * records.
 */
@Service
@Transactional(readOnly = true)
public class UnitService {

    private static final Logger log = LogManager.getLogger(UnitService.class);

    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Lists all units.
     *
     * @param activeOnly when {@code true} only active units are returned,
     *                   ordered alphabetically; otherwise all units are returned
     * @return list of unit response DTOs
     */
    public List<UnitResponse> list(boolean activeOnly) {
        List<Unit> entities = activeOnly
                ? unitRepository.findByIsActiveTrueOrderByNameAsc()
                : unitRepository.findAll();
        return entities.stream()
                .map(UnitResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns a comma-separated string of all active unit names for injection
     * into LLM extraction prompts.
     *
     * <p>Each unit is represented as {@code "name (abbreviation)"}.
     * Result is cached under {@code "units"} with the fixed key {@code "allActiveNames"}.
     *
     * @return CSV string ready to embed in a prompt
     */
    @Cacheable(value = CacheConfig.CACHE_UNITS, key = "'allActiveNames'")
    public String getAllNamesAsCSV() {
        List<Unit> active = unitRepository.findByIsActiveTrueOrderByNameAsc();
        String csv = active.stream()
                .map(u -> u.getName() + " (" + u.getAbbreviation() + ")")
                .collect(Collectors.joining(", "));
        log.debug("Built units CSV with {} entries", active.size());
        return csv;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Creates a new unit.
     *
     * @param request the validated create request
     * @return the persisted unit as a response DTO
     * @throws IllegalArgumentException if a unit with the same name or abbreviation already exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_UNITS, allEntries = true)
    public UnitResponse create(UnitRequest request) {
        assertNameUnique(null, request.getName());
        assertAbbreviationUnique(null, request.getAbbreviation());

        Unit unit = new Unit();
        unit.setName(request.getName().trim());
        unit.setAbbreviation(request.getAbbreviation().trim());
        unit.setIsActive(true);

        Unit saved = unitRepository.save(unit);
        log.info("Created unit id={} name='{}' abbreviation='{}'",
                saved.getId(), saved.getName(), saved.getAbbreviation());
        return UnitResponse.fromEntity(saved);
    }

    /**
     * Updates an existing unit.
     *
     * @param id      the UUID of the unit to update
     * @param request the validated update request
     * @return the updated unit as a response DTO
     * @throws ResourceNotFoundException if no unit with the given ID exists
     * @throws IllegalArgumentException  if the new name or abbreviation conflicts with another unit
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_UNITS, allEntries = true)
    public UnitResponse update(UUID id, UnitRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));

        assertNameUnique(id, request.getName());
        assertAbbreviationUnique(id, request.getAbbreviation());

        unit.setName(request.getName().trim());
        unit.setAbbreviation(request.getAbbreviation().trim());

        Unit saved = unitRepository.save(unit);
        log.info("Updated unit id={} name='{}' abbreviation='{}'",
                saved.getId(), saved.getName(), saved.getAbbreviation());
        return UnitResponse.fromEntity(saved);
    }

    /**
     * Deactivates (soft-deletes) a unit by setting {@code is_active = false}.
     *
     * @param id the UUID of the unit to deactivate
     * @throws ResourceNotFoundException if no unit with the given ID exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_UNITS, allEntries = true)
    public void deactivate(UUID id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
        unit.setIsActive(false);
        unitRepository.save(unit);
        log.info("Deactivated unit id={} name='{}'", id, unit.getName());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void assertNameUnique(UUID excludeId, String name) {
        unitRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                        "A unit with the name '" + name.trim() + "' already exists");
            }
        });
    }

    private void assertAbbreviationUnique(UUID excludeId, String abbreviation) {
        unitRepository.findByAbbreviationIgnoreCase(abbreviation.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                        "A unit with the abbreviation '" + abbreviation.trim() + "' already exists");
            }
        });
    }
}
