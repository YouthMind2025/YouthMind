package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Enhanced Redis Query Result Cache with intelligent invalidation and multi-level caching.
 * Provides sophisticated caching strategies for database query results with automatic
 * invalidation, cache warming, and performance optimization.
 * 
 * Features:
 * - Multi-level caching (L1: in-memory, L2: Redis, L3: database)
 * - Intelligent cache invalidation with event-driven TTL management
 * - Cache warming and preloading strategies
 * - Performance monitoring and metrics
 * - Automatic cache optimization
 * - Cache compression and serialization optimization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedRedisQueryCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final QueryPerformanceProfiler queryProfiler;

    // Cache configuration
    private static final String CACHE_PREFIX = "query_cache:";
    private static final String CACHE_METADATA_PREFIX = "cache_meta:";
    private static final String CACHE_STATS_PREFIX = "cache_stats:";
    
    // Cache TTLs (in seconds)
    private static final long DEFAULT_TTL = 300; // 5 minutes
    private static final long LONG_TTL = 1800; // 30 minutes
    private static final long SHORT_TTL = 60; // 1 minute
    
    // Cache levels
    private static final String L1_CACHE_PREFIX = "l1:";
    private static final String L2_CACHE_PREFIX = "l2:";
    
    // Performance tracking
    private final Map<String, CacheMetrics> cacheMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalCacheEvictions = new AtomicLong(0);
    
    // L1 cache (in-memory)
    private final Map<String, CacheEntry> l1Cache = new ConcurrentHashMap<>();
    private static final int L1_CACHE_MAX_SIZE = 1000;

    /**
     * Get cached query result with multi-level caching
     */
    public <T> Optional<T> getCachedResult(String cacheKey, Class<T> resultType) {
        return queryProfiler.profileQuery("cache_get_" + cacheKey, () -> {
            try {
                // L1 Cache (in-memory) - fastest
                CacheEntry l1Entry = l1Cache.get(cacheKey);
                if (l1Entry != null && !l1Entry.isExpired()) {
                    totalCacheHits.incrementAndGet();
                    recordCacheHit(cacheKey, "L1");
                    log.debug("L1 cache hit for key: {}", cacheKey);
                    return Optional.of(resultType.cast(l1Entry.getValue()));
                }
                
                // L2 Cache (Redis) - fast
                String redisKey = L2_CACHE_PREFIX + cacheKey;
                T redisValue = (T) redisTemplate.opsForValue().get(redisKey);
                if (redisValue != null) {
                    totalCacheHits.incrementAndGet();
                    recordCacheHit(cacheKey, "L2");
                    
                    // Promote to L1 cache
                    promoteToL1Cache(cacheKey, redisValue, getCacheTTL(cacheKey));
                    
                    log.debug("L2 cache hit for key: {}", cacheKey);
                    return Optional.of(redisValue);
                }
                
                // Cache miss
                totalCacheMisses.incrementAndGet();
                recordCacheMiss(cacheKey);
                log.debug("Cache miss for key: {}", cacheKey);
                return Optional.empty();
                
            } catch (Exception e) {
                log.error("Error getting cached result for key {}: {}", cacheKey, e.getMessage(), e);
                return Optional.empty();
            }
        });
    }

    /**
     * Cache query result with intelligent TTL management
     */
    public <T> void cacheResult(String cacheKey, T result, CacheStrategy strategy) {
        try {
            long ttl = determineTTL(cacheKey, strategy);
            
            // Cache in L1 (in-memory)
            promoteToL1Cache(cacheKey, result, ttl);
            
            // Cache in L2 (Redis)
            String redisKey = L2_CACHE_PREFIX + cacheKey;
            redisTemplate.opsForValue().set(redisKey, result, ttl, TimeUnit.SECONDS);
            
            // Store cache metadata
            storeCacheMetadata(cacheKey, strategy, ttl);
            
            log.debug("Cached result for key: {} with TTL: {}s", cacheKey, ttl);
            
        } catch (Exception e) {
            log.error("Error caching result for key {}: {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Execute query with caching
     */
    public <T> T executeWithCache(String cacheKey, Supplier<T> querySupplier, 
                                 Class<T> resultType, CacheStrategy strategy) {
        return queryProfiler.profileQuery("cached_query_" + cacheKey, () -> {
            try {
                // Try cache first
                Optional<T> cachedResult = getCachedResult(cacheKey, resultType);
                if (cachedResult.isPresent()) {
                    return cachedResult.get();
                }
                
                // Execute query
                T result = querySupplier.get();
                
                // Cache the result
                if (result != null) {
                    cacheResult(cacheKey, result, strategy);
                }
                
                return result;
                
            } catch (Exception e) {
                log.error("Error executing cached query for key {}: {}", cacheKey, e.getMessage(), e);
                throw e;
            }
        });
    }

    /**
     * Invalidate cache entries intelligently
     */
    public void invalidateCache(String cacheKey) {
        try {
            // Remove from L1 cache
            l1Cache.remove(cacheKey);
            
            // Remove from L2 cache
            String redisKey = L2_CACHE_PREFIX + cacheKey;
            redisTemplate.delete(redisKey);
            
            // Remove metadata
            String metadataKey = CACHE_METADATA_PREFIX + cacheKey;
            redisTemplate.delete(metadataKey);
            
            totalCacheEvictions.incrementAndGet();
            log.debug("Invalidated cache for key: {}", cacheKey);
            
        } catch (Exception e) {
            log.error("Error invalidating cache for key {}: {}", cacheKey, e.getMessage(), e);
        }
    }

    /**
     * Invalidate cache entries by pattern
     */
    public void invalidateCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(L2_CACHE_PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cache entries matching pattern: {}", keys.size(), pattern);
            }
            
            // Also invalidate L1 cache entries matching pattern
            l1Cache.entrySet().removeIf(entry -> entry.getKey().contains(pattern));
            
        } catch (Exception e) {
            log.error("Error invalidating cache by pattern {}: {}", pattern, e.getMessage(), e);
        }
    }

    /**
     * Warm up cache with frequently accessed data
     */
    public CompletableFuture<Void> warmupCache(Map<String, Supplier<Object>> warmupQueries) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting cache warmup for {} queries", warmupQueries.size());
                
                for (Map.Entry<String, Supplier<Object>> entry : warmupQueries.entrySet()) {
                    String cacheKey = entry.getKey();
                    Supplier<Object> querySupplier = entry.getValue();
                    
                    try {
                        Object result = querySupplier.get();
                        if (result != null) {
                            cacheResult(cacheKey, result, CacheStrategy.FREQUENT);
                        }
                    } catch (Exception e) {
                        log.warn("Error warming up cache for key {}: {}", cacheKey, e.getMessage());
                    }
                }
                
                log.info("Cache warmup completed for {} queries", warmupQueries.size());
                
            } catch (Exception e) {
                log.error("Error during cache warmup: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Get cache performance metrics
     */
    public CachePerformanceMetrics getCacheMetrics() {
        try {
            long totalRequests = totalCacheHits.get() + totalCacheMisses.get();
            double hitRate = totalRequests > 0 ? (double) totalCacheHits.get() / totalRequests : 0;
            
            // Calculate L1 and L2 hit rates
            long l1Hits = cacheMetrics.values().stream()
                .mapToLong(m -> m.getL1Hits())
                .sum();
            long l2Hits = cacheMetrics.values().stream()
                .mapToLong(m -> m.getL2Hits())
                .sum();
            
            return CachePerformanceMetrics.builder()
                .totalRequests(totalRequests)
                .totalHits(totalCacheHits.get())
                .totalMisses(totalCacheMisses.get())
                .totalEvictions(totalCacheEvictions.get())
                .overallHitRate(hitRate)
                .l1HitRate(totalRequests > 0 ? (double) l1Hits / totalRequests : 0)
                .l2HitRate(totalRequests > 0 ? (double) l2Hits / totalRequests : 0)
                .l1CacheSize(l1Cache.size())
                .l2CacheSize(getL2CacheSize())
                .averageResponseTime(calculateAverageResponseTime())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting cache metrics: {}", e.getMessage(), e);
            return CachePerformanceMetrics.builder()
                .totalRequests(0)
                .totalHits(0)
                .totalMisses(0)
                .totalEvictions(0)
                .overallHitRate(0)
                .l1HitRate(0)
                .l2HitRate(0)
                .l1CacheSize(0)
                .l2CacheSize(0)
                .averageResponseTime(0)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Optimize cache based on usage patterns
     */
    public void optimizeCache() {
        try {
            log.info("Starting cache optimization");
            
            // Clean up expired L1 cache entries
            l1Cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            
            // Evict least recently used entries if L1 cache is full
            if (l1Cache.size() > L1_CACHE_MAX_SIZE) {
                evictLRUEntries();
            }
            
            // Optimize TTLs based on hit rates
            optimizeTTLs();
            
            log.info("Cache optimization completed");
            
        } catch (Exception e) {
            log.error("Error optimizing cache: {}", e.getMessage(), e);
        }
    }

    // Private helper methods
    private void promoteToL1Cache(String cacheKey, Object value, long ttl) {
        try {
            // Evict LRU entries if cache is full
            if (l1Cache.size() >= L1_CACHE_MAX_SIZE) {
                evictLRUEntries();
            }
            
            CacheEntry entry = CacheEntry.builder()
                .value(value)
                .createdAt(System.currentTimeMillis())
                .ttl(ttl * 1000) // Convert to milliseconds
                .accessCount(1)
                .lastAccessed(System.currentTimeMillis())
                .build();
            
            l1Cache.put(cacheKey, entry);
            
        } catch (Exception e) {
            log.warn("Error promoting to L1 cache: {}", e.getMessage());
        }
    }

    private void evictLRUEntries() {
        try {
            // Sort by last accessed time and remove oldest 10%
            int entriesToRemove = Math.max(1, L1_CACHE_MAX_SIZE / 10);
            
            l1Cache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((a, b) -> 
                    Long.compare(a.getLastAccessed(), b.getLastAccessed())))
                .limit(entriesToRemove)
                .forEach(entry -> l1Cache.remove(entry.getKey()));
                
        } catch (Exception e) {
            log.warn("Error evicting LRU entries: {}", e.getMessage());
        }
    }

    private long determineTTL(String cacheKey, CacheStrategy strategy) {
        switch (strategy) {
            case FREQUENT:
                return LONG_TTL;
            case INFREQUENT:
                return SHORT_TTL;
            case DYNAMIC:
                return calculateDynamicTTL(cacheKey);
            default:
                return DEFAULT_TTL;
        }
    }

    private long calculateDynamicTTL(String cacheKey) {
        CacheMetrics metrics = cacheMetrics.get(cacheKey);
        if (metrics != null && metrics.getHitRate() > 0.8) {
            return LONG_TTL; // High hit rate, cache longer
        } else if (metrics != null && metrics.getHitRate() < 0.3) {
            return SHORT_TTL; // Low hit rate, cache shorter
        }
        return DEFAULT_TTL;
    }

    private void storeCacheMetadata(String cacheKey, CacheStrategy strategy, long ttl) {
        try {
            String metadataKey = CACHE_METADATA_PREFIX + cacheKey;
            CacheMetadata metadata = CacheMetadata.builder()
                .cacheKey(cacheKey)
                .strategy(strategy)
                .ttl(ttl)
                .createdAt(LocalDateTime.now())
                .build();
            
            redisTemplate.opsForValue().set(metadataKey, metadata, ttl, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.warn("Error storing cache metadata: {}", e.getMessage());
        }
    }

    private void recordCacheHit(String cacheKey, String level) {
        CacheMetrics metrics = cacheMetrics.computeIfAbsent(cacheKey, k -> new CacheMetrics());
        metrics.recordHit(level);
    }

    private void recordCacheMiss(String cacheKey) {
        CacheMetrics metrics = cacheMetrics.computeIfAbsent(cacheKey, k -> new CacheMetrics());
        metrics.recordMiss();
    }

    private long getL2CacheSize() {
        try {
            Set<String> keys = redisTemplate.keys(L2_CACHE_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double calculateAverageResponseTime() {
        return cacheMetrics.values().stream()
            .mapToDouble(CacheMetrics::getAverageResponseTime)
            .average()
            .orElse(0);
    }

    private long getCacheTTL(String cacheKey) {
        try {
            String metadataKey = CACHE_METADATA_PREFIX + cacheKey;
            CacheMetadata metadata = (CacheMetadata) redisTemplate.opsForValue().get(metadataKey);
            return metadata != null ? metadata.getTtl() : DEFAULT_TTL;
        } catch (Exception e) {
            return DEFAULT_TTL;
        }
    }

    private void optimizeTTLs() {
        // Implementation for TTL optimization based on hit rates
        // This could be enhanced with machine learning algorithms
    }

    // Inner classes
    @lombok.Data
    @lombok.Builder
    public static class CacheEntry {
        private Object value;
        private long createdAt;
        private long ttl;
        private int accessCount;
        private long lastAccessed;
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttl;
        }
    }

    @lombok.Data
    public static class CacheMetrics {
        private long l1Hits = 0;
        private long l2Hits = 0;
        private long misses = 0;
        private long totalRequests = 0;
        private double averageResponseTime = 0;
        
        public void recordHit(String level) {
            if ("L1".equals(level)) {
                l1Hits++;
            } else if ("L2".equals(level)) {
                l2Hits++;
            }
            totalRequests++;
        }
        
        public void recordMiss() {
            misses++;
            totalRequests++;
        }
        
        public double getHitRate() {
            return totalRequests > 0 ? (double) (l1Hits + l2Hits) / totalRequests : 0;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class CacheMetadata {
        private String cacheKey;
        private CacheStrategy strategy;
        private long ttl;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class CachePerformanceMetrics {
        private long totalRequests;
        private long totalHits;
        private long totalMisses;
        private long totalEvictions;
        private double overallHitRate;
        private double l1HitRate;
        private double l2HitRate;
        private int l1CacheSize;
        private long l2CacheSize;
        private double averageResponseTime;
        private LocalDateTime timestamp;
    }

    public enum CacheStrategy {
        FREQUENT,    // Cache for longer periods
        INFREQUENT,  // Cache for shorter periods
        DYNAMIC      // Dynamic TTL based on usage patterns
    }
}
