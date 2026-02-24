package com.tradeintel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-process Caffeine cache configuration.
 *
 * <p>This application intentionally avoids Redis (see CLAUDE.md). A Caffeine
 * {@link CacheManager} provides low-latency, in-heap caching suitable for the
 * expected small user base.
 *
 * <p>Two caches are defined:
 * <ul>
 *   <li><b>jargon</b>     — caches the verified jargon dictionary used by
 *       {@code JargonExpander} during message pre-processing. TTL is configurable
 *       via {@code app.cache.jargon-ttl-minutes} (default 10 minutes).</li>
 *   <li><b>categories</b> — caches the admin-managed category list injected into
 *       LLM extraction prompts. TTL is configurable via
 *       {@code app.cache.categories-ttl-minutes} (default 30 minutes).</li>
 * </ul>
 *
 * <p>Additional caches can be declared by adding their names to the list in
 * {@link #cacheManager()} and configuring per-cache specifications via
 * {@link CaffeineCacheManager#registerCustomCache(String, com.github.benmanes.caffeine.cache.Cache)}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LogManager.getLogger(CacheConfig.class);

    /** Cache name for the verified jargon dictionary. */
    public static final String CACHE_JARGON     = "jargon";

    /** Cache name for the admin-managed categories list. */
    public static final String CACHE_CATEGORIES = "categories";

    @Value("${app.cache.jargon-ttl-minutes:10}")
    private long jargonTtlMinutes;

    @Value("${app.cache.categories-ttl-minutes:30}")
    private long categoriesTtlMinutes;

    /**
     * Creates the primary {@link CacheManager}.
     *
     * <p>Each cache is backed by an independently configured Caffeine instance so
     * that TTL values can differ without sharing a global specification.
     *
     * @return configured Caffeine cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Register individual caches with their own TTL specifications.
        manager.registerCustomCache(CACHE_JARGON,
                Caffeine.newBuilder()
                        .expireAfterWrite(jargonTtlMinutes, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .recordStats()
                        .build()
        );

        manager.registerCustomCache(CACHE_CATEGORIES,
                Caffeine.newBuilder()
                        .expireAfterWrite(categoriesTtlMinutes, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build()
        );

        // Allow Spring to create dynamic caches for any @Cacheable annotation
        // that references a cache name not explicitly registered above.
        manager.setAllowNullValues(false);

        log.info("Caffeine cache manager initialised: jargonTtl={}m categoriesTtl={}m",
                jargonTtlMinutes, categoriesTtlMinutes);

        return manager;
    }
}
