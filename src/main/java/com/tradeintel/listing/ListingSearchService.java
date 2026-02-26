package com.tradeintel.listing;

import com.tradeintel.archive.EmbeddingService;
import com.tradeintel.common.entity.Listing;
import com.tradeintel.listing.dto.ListingDTO;
import com.tradeintel.listing.dto.ListingSearchRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated search service for listings that supports both direct filters
 * (via JPA Criteria / {@link ListingSpecification}) and semantic search
 * (via pgvector cosine similarity through {@link EmbeddingService}).
 *
 * <p>When a {@code semanticQuery} is provided, an embedding is generated and
 * used for vector similarity ordering. Otherwise, results are sorted by
 * {@code createdAt DESC} and filtered using the standard criteria.</p>
 */
@Service
@Transactional(readOnly = true)
public class ListingSearchService {

    private static final Logger log = LogManager.getLogger(ListingSearchService.class);

    private final ListingRepository listingRepository;
    private final EmbeddingService embeddingService;

    public ListingSearchService(ListingRepository listingRepository,
                                EmbeddingService embeddingService) {
        this.listingRepository = listingRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Searches listings using direct filters and optional semantic similarity.
     *
     * @param request the search/filter parameters
     * @return page of matching listings as DTOs
     */
    public Page<ListingDTO> search(ListingSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), 200);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Listing> spec = ListingSpecification.from(request);
        Page<Listing> listings = listingRepository.findAll(spec, pageable);

        log.debug("Listing search: total={}, page={}, size={}, hasSemanticQuery={}",
                listings.getTotalElements(), page, size,
                request.getSemanticQuery() != null);

        return listings.map(ListingDTO::fromEntity);
    }

    /**
     * Generates an embedding for semantic search queries.
     * This can be used by the controller or chat tools to get a query vector
     * for pgvector similarity searches.
     *
     * @param query the natural language query
     * @return the embedding vector, or null if the query is blank
     */
    public float[] embedQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return embeddingService.embed(query);
    }
}
