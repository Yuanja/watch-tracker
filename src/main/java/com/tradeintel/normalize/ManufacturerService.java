package com.tradeintel.normalize;

import com.tradeintel.admin.AuditService;
import com.tradeintel.common.entity.Manufacturer;
import com.tradeintel.common.exception.ResourceNotFoundException;
import com.tradeintel.config.CacheConfig;
import com.tradeintel.normalize.dto.ManufacturerRequest;
import com.tradeintel.normalize.dto.ManufacturerResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for the admin-managed {@code Manufacturer} normalized value table.
 *
 * <p>Active manufacturer lists are cached in Caffeine (cache name
 * {@code "manufacturers"}) and evicted on any write so that the LLM extraction
 * prompt always reflects current state.
 *
 * <p>Deactivation is a soft-delete: the {@code is_active} flag is set to
 * {@code false}, preserving referential integrity with existing {@code listings}
 * records.
 */
@Service
@Transactional(readOnly = true)
public class ManufacturerService {

    private static final Logger log = LogManager.getLogger(ManufacturerService.class);

    private final ManufacturerRepository manufacturerRepository;
    private final AuditService auditService;

    public ManufacturerService(ManufacturerRepository manufacturerRepository,
                               AuditService auditService) {
        this.manufacturerRepository = manufacturerRepository;
        this.auditService = auditService;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Lists all manufacturers.
     *
     * @param activeOnly when {@code true} only active manufacturers are returned,
     *                   ordered alphabetically; otherwise all manufacturers are returned
     * @return list of manufacturer response DTOs
     */
    public List<ManufacturerResponse> list(boolean activeOnly) {
        List<Manufacturer> entities = activeOnly
                ? manufacturerRepository.findByIsActiveTrueOrderByNameAsc()
                : manufacturerRepository.findAll();
        return entities.stream()
                .map(ManufacturerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns a comma-separated string of all active manufacturer names and their aliases
     * for injection into LLM extraction prompts.
     *
     * <p>Each manufacturer is represented as {@code "Canonical Name (alias1, alias2)"}
     * when aliases are present, or just the canonical name when there are none.
     *
     * <p>Result is cached under {@code "manufacturers"} with the fixed key
     * {@code "allNamesWithAliases"}.
     *
     * @return CSV string ready to embed in a prompt
     */
    @Cacheable(value = CacheConfig.CACHE_MANUFACTURERS, key = "'allNamesWithAliases'")
    public String getAllNamesWithAliasesAsCSV() {
        List<Manufacturer> active = manufacturerRepository.findByIsActiveTrueOrderByNameAsc();
        String csv = active.stream()
                .map(m -> {
                    if (m.getAliases() != null && m.getAliases().length > 0) {
                        String aliasStr = Arrays.stream(m.getAliases())
                                .collect(Collectors.joining(", "));
                        return m.getName() + " (" + aliasStr + ")";
                    }
                    return m.getName();
                })
                .collect(Collectors.joining(", "));
        log.debug("Built manufacturers CSV with {} entries", active.size());
        return csv;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Creates a new manufacturer.
     *
     * @param request the validated create request
     * @return the persisted manufacturer as a response DTO
     * @throws IllegalArgumentException if a manufacturer with the same name already exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MANUFACTURERS, allEntries = true)
    public ManufacturerResponse create(ManufacturerRequest request) {
        assertNameUnique(null, request.getName());

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(request.getName().trim());
        manufacturer.setAliases(request.getAliases());
        manufacturer.setWebsite(request.getWebsite());
        manufacturer.setIsActive(true);

        Manufacturer saved = manufacturerRepository.save(manufacturer);
        log.info("Created manufacturer id={} name='{}'", saved.getId(), saved.getName());
        auditService.log(null, "manufacturer.create", "Manufacturer", saved.getId(), null, null, null);
        return ManufacturerResponse.fromEntity(saved);
    }

    /**
     * Updates an existing manufacturer.
     *
     * @param id      the UUID of the manufacturer to update
     * @param request the validated update request
     * @return the updated manufacturer as a response DTO
     * @throws ResourceNotFoundException if no manufacturer with the given ID exists
     * @throws IllegalArgumentException  if the new name conflicts with another manufacturer
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MANUFACTURERS, allEntries = true)
    public ManufacturerResponse update(UUID id, ManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));

        assertNameUnique(id, request.getName());

        manufacturer.setName(request.getName().trim());
        manufacturer.setAliases(request.getAliases());
        manufacturer.setWebsite(request.getWebsite());

        Manufacturer saved = manufacturerRepository.save(manufacturer);
        log.info("Updated manufacturer id={} name='{}'", saved.getId(), saved.getName());
        auditService.log(null, "manufacturer.update", "Manufacturer", saved.getId(), null, null, null);
        return ManufacturerResponse.fromEntity(saved);
    }

    /**
     * Deactivates (soft-deletes) a manufacturer by setting {@code is_active = false}.
     *
     * @param id the UUID of the manufacturer to deactivate
     * @throws ResourceNotFoundException if no manufacturer with the given ID exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_MANUFACTURERS, allEntries = true)
    public void deactivate(UUID id) {
        Manufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));
        manufacturer.setIsActive(false);
        manufacturerRepository.save(manufacturer);
        log.info("Deactivated manufacturer id={} name='{}'", id, manufacturer.getName());
        auditService.log(null, "manufacturer.deactivate", "Manufacturer", id, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void assertNameUnique(UUID excludeId, String name) {
        manufacturerRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(
                        "A manufacturer with the name '" + name.trim() + "' already exists");
            }
        });
    }
}
