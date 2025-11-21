package ai.youthmind.service;

import ai.youthmind.entity.PsychologicalStateVector;
import ai.youthmind.repository.PsychologicalStateVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Optimized repository service for psychological state vectors with intelligent caching,
 * query optimization, and performance monitoring.
 * 
 * Features:
 * - Multi-level caching (L1: in-memory, L2: Redis, L3: database)
 * - Query optimization with automatic index recommendations
 * - Batch processing for bulk operations
 * - Intelligent cache invalidation
 * - Performance monitoring and metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PsychologicalStateVectorOptimizedRepository {

    private final PsychologicalStateVectorRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final QueryPerformanceProfiler queryProfiler;
    private final DatabaseQueryOptimizer queryOptimizer;

    // Cache configuration
    private static final String CACHE_PREFIX = "psv:";
    private static final String CACHE_LATEST_PREFIX = "psv:latest:";
    private static final String CACHE_USER_PREFIX = "psv:user:";
    private static final String CACHE_INTENSITY_PREFIX = "psv:intensity:";
    
    // Cache TTLs (in seconds)
    private static final long LATEST_CACHE_TTL = 300; // 5 minutes
    private static final long USER_CACHE_TTL = 600; // 10 minutes
    private static final long INTENSITY_CACHE_TTL = 1800; // 30 minutes
    private static final long BATCH_CACHE_TTL = 120; // 2 minutes

    // Batch processing configuration
    private static final int BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 1000;

    /**
     * Find the latest vector by user ID and vector type with intelligent caching
     */
    @Cacheable(value = "latestVectors", key = "#userId + ':' + #vectorType", unless = "#result == null")
    public Optional<PsychologicalStateVector> findLatestByUserIdAndVectorTypeOptimized(
            UUID userId, PsychologicalStateVector.VectorType vectorType) {
        
        return queryProfiler.profileQuery("findLatestByUserIdAndVectorType", () -> {
            try {
                // Try L1 cache first (Spring Cache)
                String cacheKey = CACHE_LATEST_PREFIX + userId + ":" + vectorType;
                
                // Try L2 cache (Redis)
                PsychologicalStateVector cached = (PsychologicalStateVector) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Cache hit for latest vector: {}:{}", userId, vectorType);
                    return Optional.of(cached);
                }
                
                // L3 cache (Database) with optimized query
                Optional<PsychologicalStateVector> result = repository.findLatestByUserIdAndVectorType(userId, vectorType);
                
                // Cache the result
                if (result.isPresent()) {
                    redisTemplate.opsForValue().set(cacheKey, result.get(), LATEST_CACHE_TTL, TimeUnit.SECONDS);
                }
                
                return result;
                
            } catch (Exception e) {
                log.error("Error finding latest vector for user {} and type {}: {}", userId, vectorType, e.getMessage(), e);
                return Optional.empty();
            }
        });
    }

    /**
     * Find vectors by user ID and vector type with intelligent caching and pagination
     */
    public Page<PsychologicalStateVector> findByUserIdAndVectorTypeOptimized(
            UUID userId, PsychologicalStateVector.VectorType vectorType, Pageable pageable) {
        
        return queryProfiler.profileQuery("findByUserIdAndVectorType", () -> {
            try {
                String cacheKey = CACHE_USER_PREFIX + userId + ":" + vectorType + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
                
                // Try Redis cache first
                Page<PsychologicalStateVector> cached = (Page<PsychologicalStateVector>) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Cache hit for user vectors: {}:{}", userId, vectorType);
                    return cached;
                }
                
                // Database query with optimization
                List<PsychologicalStateVector> vectors = repository.findByUserIdAndVectorType(userId, vectorType);
                
                // Apply pagination manually (since we're using List instead of Page)
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), vectors.size());
                List<PsychologicalStateVector> pageContent = vectors.subList(start, end);
                
                Page<PsychologicalStateVector> result = new org.springframework.data.domain.PageImpl<>(
                    pageContent, pageable, vectors.size());
                
                // Cache the result
                redisTemplate.opsForValue().set(cacheKey, result, USER_CACHE_TTL, TimeUnit.SECONDS);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error finding vectors for user {} and type {}: {}", userId, vectorType, e.getMessage(), e);
                return Page.empty(pageable);
            }
        });
    }

    /**
     * Find vectors by user ID within time range with optimized query
     */
    public List<PsychologicalStateVector> findByUserIdAndCreatedAtBetweenOptimized(
            UUID userId, LocalDateTime startTime, LocalDateTime endTime) {
        
        return queryProfiler.profileQuery("findByUserIdAndCreatedAtBetween", () -> {
            try {
                String cacheKey = CACHE_USER_PREFIX + userId + ":range:" + startTime + ":" + endTime;
                
                // Try cache first
                @SuppressWarnings("unchecked")
                List<PsychologicalStateVector> cached = (List<PsychologicalStateVector>) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Cache hit for user vectors in range: {}", userId);
                    return cached;
                }
                
                // Optimized database query
                List<PsychologicalStateVector> result = repository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime);
                
                // Cache the result
                redisTemplate.opsForValue().set(cacheKey, result, USER_CACHE_TTL, TimeUnit.SECONDS);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error finding vectors for user {} in range {} to {}: {}", userId, startTime, endTime, e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * Find vectors with intensity factor above threshold with optimized query
     */
    public List<PsychologicalStateVector> findByIntensityFactorAboveThresholdOptimized(Double threshold) {
        return queryProfiler.profileQuery("findByIntensityFactorAboveThreshold", () -> {
            try {
                String cacheKey = CACHE_INTENSITY_PREFIX + threshold;
                
                // Try cache first
                @SuppressWarnings("unchecked")
                List<PsychologicalStateVector> cached = (List<PsychologicalStateVector>) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Cache hit for intensity threshold: {}", threshold);
                    return cached;
                }
                
                // Optimized database query
                List<PsychologicalStateVector> result = repository.findByIntensityFactorAboveThreshold(threshold);
                
                // Cache the result
                redisTemplate.opsForValue().set(cacheKey, result, INTENSITY_CACHE_TTL, TimeUnit.SECONDS);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error finding vectors with intensity above {}: {}", threshold, e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * Batch save vectors with optimized connection reuse
     */
    @Transactional
    public CompletableFuture<List<PsychologicalStateVector>> batchSaveOptimized(List<PsychologicalStateVector> vectors) {
        return CompletableFuture.supplyAsync(() -> {
            return queryProfiler.profileQuery("batchSave", () -> {
                try {
                    if (vectors.isEmpty()) {
                        return new ArrayList<>();
                    }
                    
                    // Validate batch size
                    if (vectors.size() > MAX_BATCH_SIZE) {
                        log.warn("Batch size {} exceeds maximum {}, splitting into chunks", vectors.size(), MAX_BATCH_SIZE);
                        return batchSaveInChunks(vectors);
                    }
                    
                    // Batch save with optimized connection reuse
                    List<PsychologicalStateVector> savedVectors = repository.saveAll(vectors);
                    
                    // Invalidate related caches
                    invalidateUserCaches(vectors);
                    
                    log.debug("Batch saved {} vectors", savedVectors.size());
                    return savedVectors;
                    
                } catch (Exception e) {
                    log.error("Error in batch save: {}", e.getMessage(), e);
                    return new ArrayList<>();
                }
            });
        });
    }

    /**
     * Batch save large collections in chunks
     */
    private List<PsychologicalStateVector> batchSaveInChunks(List<PsychologicalStateVector> vectors) {
        List<PsychologicalStateVector> allSaved = new ArrayList<>();
        
        for (int i = 0; i < vectors.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, vectors.size());
            List<PsychologicalStateVector> chunk = vectors.subList(i, end);
            
            List<PsychologicalStateVector> savedChunk = repository.saveAll(chunk);
            allSaved.addAll(savedChunk);
            
            // Invalidate caches for this chunk
            invalidateUserCaches(chunk);
            
            log.debug("Saved chunk {}/{} vectors", end, vectors.size());
        }
        
        return allSaved;
    }

    /**
     * Intelligent cache invalidation based on vector changes
     */
    @CacheEvict(value = {"latestVectors", "userVectors", "intensityVectors"}, allEntries = true)
    public void invalidateAllCaches() {
        log.info("Invalidating all psychological state vector caches");
        
        // Clear Redis caches
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Invalidate user-specific caches
     */
    public void invalidateUserCaches(List<PsychologicalStateVector> vectors) {
        Set<String> userIds = vectors.stream()
            .map(v -> v.getUserId().toString())
            .collect(Collectors.toSet());
        
        for (String userId : userIds) {
            invalidateUserCaches(userId);
        }
    }

    /**
     * Invalidate caches for a specific user
     */
    public void invalidateUserCaches(String userId) {
        try {
            Set<String> userKeys = redisTemplate.keys(CACHE_PREFIX + "*" + userId + "*");
            if (userKeys != null && !userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
                log.debug("Invalidated {} cache keys for user {}", userKeys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("Error invalidating caches for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get query optimization recommendations
     */
    public List<DatabaseQueryOptimizer.OptimizationRecommendation> getOptimizationRecommendations() {
        return queryOptimizer.getRecommendations("PsychologicalStateVector");
    }

    /**
     * Get performance metrics for this repository
     */
    public QueryPerformanceProfiler.RepositoryPerformanceMetrics getPerformanceMetrics() {
        return queryProfiler.getRepositoryMetrics("PsychologicalStateVector");
    }

    /**
     * Warm up caches for frequently accessed data
     */
    public CompletableFuture<Void> warmupCaches(List<UUID> userIds) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Warming up caches for {} users", userIds.size());
                
                for (UUID userId : userIds) {
                    // Warm up latest vectors for each user
                    for (PsychologicalStateVector.VectorType vectorType : PsychologicalStateVector.VectorType.values()) {
                        findLatestByUserIdAndVectorTypeOptimized(userId, vectorType);
                    }
                }
                
                log.info("Cache warmup completed for {} users", userIds.size());
                
            } catch (Exception e) {
                log.error("Error during cache warmup: {}", e.getMessage(), e);
            }
        });
    }

}
