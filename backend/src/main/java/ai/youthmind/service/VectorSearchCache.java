package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Intelligent Vector Search Cache with Redis integration.
 * Provides multi-level caching with intelligent TTL strategies,
 * cache warming, and performance optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchCache {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // Cache configuration
    private static final String CACHE_PREFIX = "vector_search:";
    private static final String METADATA_PREFIX = "vector_metadata:";
    private static final String STATS_PREFIX = "vector_stats:";
    
    // TTL configurations (in minutes)
    private static final int DEFAULT_TTL = 5;
    private static final int HIGH_FREQUENCY_TTL = 15;
    private static final int LOW_FREQUENCY_TTL = 2;
    private static final int METADATA_TTL = 60;
    
    // Cache statistics
    private final Map<String, CacheStats> cacheStats = new HashMap<>();

    /**
     * Cache search results with intelligent TTL
     */
    public CompletableFuture<Void> cacheSearchResults(String cacheKey, List<Object> results, String collectionName) {
        return CompletableFuture.runAsync(() -> {
            try {
                String fullCacheKey = CACHE_PREFIX + collectionName + ":" + cacheKey;
                
                // Determine TTL based on result quality and frequency
                int ttl = calculateIntelligentTTL(results, collectionName);
                
                // Cache the results
                redisTemplate.opsForValue().set(fullCacheKey, results, ttl, TimeUnit.MINUTES);
                
                // Cache metadata for cache warming
                cacheSearchMetadata(fullCacheKey, results.size(), ttl);
                
                // Update cache statistics
                updateCacheStats(collectionName, true, results.size());
                
                log.debug("Cached search results for {} with TTL {} minutes", cacheKey, ttl);
                
            } catch (Exception e) {
                log.error("Error caching search results for {}: {}", cacheKey, e.getMessage(), e);
                updateCacheStats(collectionName, false, 0);
            }
        });
    }

    /**
     * Retrieve cached search results
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Object>> getCachedResults(String cacheKey, String collectionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String fullCacheKey = CACHE_PREFIX + collectionName + ":" + cacheKey;
                
                List<Object> results = (List<Object>) redisTemplate.opsForValue().get(fullCacheKey);
                
                if (results != null) {
                    // Update cache statistics
                    updateCacheStats(collectionName, true, results.size());
                    
                    // Update access metadata for cache warming
                    updateAccessMetadata(fullCacheKey);
                    
                    log.debug("Cache hit for search: {}", cacheKey);
                    return results;
                } else {
                    // Update cache statistics
                    updateCacheStats(collectionName, false, 0);
                    
                    log.debug("Cache miss for search: {}", cacheKey);
                    return null;
                }
                
            } catch (Exception e) {
                log.error("Error retrieving cached results for {}: {}", cacheKey, e.getMessage(), e);
                updateCacheStats(collectionName, false, 0);
                return null;
            }
        });
    }

    /**
     * Cache vector embeddings for reuse
     */
    public CompletableFuture<Void> cacheVectorEmbedding(String contentHash, List<Float> embedding) {
        return CompletableFuture.runAsync(() -> {
            try {
                String cacheKey = METADATA_PREFIX + "embedding:" + contentHash;
                
                // Cache embedding with longer TTL since they don't change often
                redisTemplate.opsForValue().set(cacheKey, embedding, METADATA_TTL, TimeUnit.MINUTES);
                
                log.debug("Cached vector embedding for content hash: {}", contentHash);
                
            } catch (Exception e) {
                log.error("Error caching vector embedding for {}: {}", contentHash, e.getMessage(), e);
            }
        });
    }

    /**
     * Retrieve cached vector embedding
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Float>> getCachedEmbedding(String contentHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String cacheKey = METADATA_PREFIX + "embedding:" + contentHash;
                
                List<Float> embedding = (List<Float>) redisTemplate.opsForValue().get(cacheKey);
                
                if (embedding != null) {
                    log.debug("Cache hit for embedding: {}", contentHash);
                    return embedding;
                } else {
                    log.debug("Cache miss for embedding: {}", contentHash);
                    return null;
                }
                
            } catch (Exception e) {
                log.error("Error retrieving cached embedding for {}: {}", contentHash, e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Warm cache with frequently accessed data
     */
    public CompletableFuture<Void> warmCache(String collectionName, List<String> frequentQueries) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting cache warming for collection: {}", collectionName);
                
                int warmedCount = 0;
                for (String query : frequentQueries) {
                    String cacheKey = generateCacheKey(query);
                    
                    // Check if already cached
                    List<Object> existing = getCachedResults(cacheKey, collectionName).get();
                    if (existing == null) {
                        // This would typically trigger a search and cache the results
                        // For now, we'll just log the warming attempt
                        log.debug("Cache warming query: {}", query);
                        warmedCount++;
                    }
                }
                
                log.info("Cache warming completed for {}: {} queries warmed", collectionName, warmedCount);
                
            } catch (Exception e) {
                log.error("Error warming cache for collection {}: {}", collectionName, e.getMessage(), e);
            }
        });
    }

    /**
     * Invalidate cache for specific collection
     */
    public CompletableFuture<Void> invalidateCollectionCache(String collectionName) {
        return CompletableFuture.runAsync(() -> {
            try {
                String pattern = CACHE_PREFIX + collectionName + ":*";
                Set<String> keys = redisTemplate.keys(pattern);
                
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Invalidated {} cache entries for collection: {}", keys.size(), collectionName);
                }
                
            } catch (Exception e) {
                log.error("Error invalidating cache for collection {}: {}", collectionName, e.getMessage(), e);
            }
        });
    }

    /**
     * Clear all vector search cache
     */
    public CompletableFuture<Void> clearAllCache() {
        return CompletableFuture.runAsync(() -> {
            try {
                String pattern = CACHE_PREFIX + "*";
                Set<String> keys = redisTemplate.keys(pattern);
                
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Cleared {} vector search cache entries", keys.size());
                }
                
                // Clear metadata cache
                String metadataPattern = METADATA_PREFIX + "*";
                Set<String> metadataKeys = redisTemplate.keys(metadataPattern);
                
                if (metadataKeys != null && !metadataKeys.isEmpty()) {
                    redisTemplate.delete(metadataKeys);
                    log.info("Cleared {} vector metadata cache entries", metadataKeys.size());
                }
                
            } catch (Exception e) {
                log.error("Error clearing all cache: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Get cache statistics
     */
    public Map<String, CacheStats> getCacheStatistics() {
        return new HashMap<>(cacheStats);
    }

    /**
     * Get cache hit rate for a collection
     */
    public double getCacheHitRate(String collectionName) {
        CacheStats stats = cacheStats.get(collectionName);
        if (stats == null || stats.getTotalRequests() == 0) {
            return 0.0;
        }
        
        return (double) stats.getCacheHits() / stats.getTotalRequests();
    }

    /**
     * Calculate intelligent TTL based on result quality and access patterns
     */
    private int calculateIntelligentTTL(List<Object> results, String collectionName) {
        // Base TTL
        int baseTTL = DEFAULT_TTL;
        
        // Adjust based on result count (more results = higher quality = longer cache)
        if (results.size() > 10) {
            baseTTL = HIGH_FREQUENCY_TTL;
        } else if (results.size() < 3) {
            baseTTL = LOW_FREQUENCY_TTL;
        }
        
        // Adjust based on collection type
        switch (collectionName.toLowerCase()) {
            case "posts":
                return Math.min(baseTTL + 5, 20); // Posts change frequently
            case "comments":
                return Math.min(baseTTL + 3, 15); // Comments change moderately
            case "moderation":
                return Math.min(baseTTL + 10, 30); // Moderation data is more stable
            default:
                return baseTTL;
        }
    }

    /**
     * Generate cache key from query
     */
    private String generateCacheKey(String query) {
        return String.valueOf(query.hashCode());
    }

    /**
     * Cache search metadata for cache warming
     */
    private void cacheSearchMetadata(String cacheKey, int resultCount, int ttl) {
        try {
            Map<String, Object> metadata = Map.of(
                "result_count", resultCount,
                "ttl_minutes", ttl,
                "cached_at", System.currentTimeMillis(),
                "access_count", 0
            );
            
            String metadataKey = cacheKey + ":metadata";
            redisTemplate.opsForValue().set(metadataKey, metadata, ttl, TimeUnit.MINUTES);
            
        } catch (Exception e) {
            log.error("Error caching search metadata: {}", e.getMessage(), e);
        }
    }

    /**
     * Update access metadata for cache warming
     */
    private void updateAccessMetadata(String cacheKey) {
        try {
            String metadataKey = cacheKey + ":metadata";
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) redisTemplate.opsForValue().get(metadataKey);
            
            if (metadata != null) {
                int accessCount = (Integer) metadata.getOrDefault("access_count", 0);
                metadata.put("access_count", accessCount + 1);
                metadata.put("last_accessed", System.currentTimeMillis());
                
                redisTemplate.opsForValue().set(metadataKey, metadata);
            }
            
        } catch (Exception e) {
            log.error("Error updating access metadata: {}", e.getMessage(), e);
        }
    }

    /**
     * Update cache statistics
     */
    private void updateCacheStats(String collectionName, boolean cacheHit, int resultCount) {
        CacheStats stats = cacheStats.computeIfAbsent(collectionName, k -> new CacheStats());
        
        stats.setTotalRequests(stats.getTotalRequests() + 1);
        if (cacheHit) {
            stats.setCacheHits(stats.getCacheHits() + 1);
        }
        stats.setTotalResults(stats.getTotalResults() + resultCount);
        stats.setLastAccessTime(System.currentTimeMillis());
        
        // Calculate hit rate
        stats.setHitRate((double) stats.getCacheHits() / stats.getTotalRequests());
    }

    /**
     * Cache statistics DTO
     */
    @lombok.Data
    public static class CacheStats {
        private long totalRequests = 0L;
        private long cacheHits = 0L;
        private long totalResults = 0L;
        private double hitRate = 0.0;
        private long lastAccessTime = 0L;
    }
}
