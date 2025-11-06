package ai.youthmind.controller;

import ai.youthmind.service.*;
import ai.youthmind.util.AuthenticationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for Multi-Level Cache management and monitoring.
 * Provides endpoints for:
 * - Cache operations and management
 * - Performance metrics and monitoring
 * - Cache warming and optimization
 * - Security and access control
 * - Partition management and statistics
 */
@RestController
@RequestMapping("/v1/cache")
@RequiredArgsConstructor
@Slf4j
public class MultiLevelCacheController {

    private final IntelligentCacheManager cacheManager;
    private final CacheCompressionService compressionService;
    private final CachePartitionManager partitionManager;
    private final CacheSecurityManager securityManager;

    /**
     * Get cache metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<IntelligentCacheManager.CacheMetrics> getCacheMetrics() {
        try {
            IntelligentCacheManager.CacheMetrics metrics = cacheManager.getMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting cache metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get cache health status
     */
    @GetMapping("/health")
    public ResponseEntity<IntelligentCacheManager.CacheHealthStatus> getCacheHealth() {
        try {
            IntelligentCacheManager.CacheHealthStatus health = cacheManager.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting cache health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get cached data
     */
    @GetMapping("/data/{key}")
    public ResponseEntity<Object> getCachedData(@PathVariable String key, @RequestParam String type) {
        try {
            // Get current user ID from security context
            String userId = AuthenticationUtils.getCurrentUserId().toString();
            Optional<Object> data = cacheManager.get(key, Object.class, IntelligentCacheManager.CacheLevel.ANY, userId).join();
            return data.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting cached data for key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Put data in cache
     */
    @PostMapping("/data/{key}")
    public ResponseEntity<String> putCachedData(
            @PathVariable String key,
            @RequestBody Object data,
            @RequestParam(defaultValue = "AUTO") String level,
            @RequestParam(defaultValue = "300") long ttlSeconds) {
        try {
            // Get current user ID from security context
            String userId = AuthenticationUtils.getCurrentUserId().toString();
            IntelligentCacheManager.CacheLevel cacheLevel = IntelligentCacheManager.CacheLevel.valueOf(level);
            Duration ttl = Duration.ofSeconds(ttlSeconds);
            
            cacheManager.put(key, data, cacheLevel, ttl, userId).join();
            return ResponseEntity.ok("Data cached successfully");
        } catch (Exception e) {
            log.error("Error putting cached data for key: {}", key, e);
            return ResponseEntity.internalServerError().body("Error caching data: " + e.getMessage());
        }
    }

    /**
     * Invalidate cache
     */
    @DeleteMapping("/data/{key}")
    public ResponseEntity<String> invalidateCache(@PathVariable String key) {
        try {
            cacheManager.invalidate(key).join();
            return ResponseEntity.ok("Cache invalidated successfully");
        } catch (Exception e) {
            log.error("Error invalidating cache for key: {}", key, e);
            return ResponseEntity.internalServerError().body("Error invalidating cache: " + e.getMessage());
        }
    }

    /**
     * Invalidate cache by pattern
     */
    @DeleteMapping("/data/pattern/{pattern}")
    public ResponseEntity<String> invalidateCacheByPattern(@PathVariable String pattern) {
        try {
            cacheManager.invalidateByPattern(pattern).join();
            return ResponseEntity.ok("Cache pattern invalidated successfully");
        } catch (Exception e) {
            log.error("Error invalidating cache pattern: {}", pattern, e);
            return ResponseEntity.internalServerError().body("Error invalidating cache pattern: " + e.getMessage());
        }
    }

    /**
     * Get partition performance summary
     */
    @GetMapping("/partitions/performance")
    public ResponseEntity<CachePartitionManager.PartitionPerformanceSummary> getPartitionPerformance() {
        try {
            CachePartitionManager.PartitionPerformanceSummary summary = partitionManager.getPerformanceSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error getting partition performance summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get partition statistics
     */
    @GetMapping("/partitions/statistics")
    public ResponseEntity<Map<CachePartitionManager.CachePartition, CachePartitionManager.PartitionStats>> getPartitionStatistics() {
        try {
            Map<CachePartitionManager.CachePartition, CachePartitionManager.PartitionStats> stats = partitionManager.getPartitionStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting partition statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get partition configuration
     */
    @GetMapping("/partitions/{partition}/configuration")
    public ResponseEntity<CachePartitionManager.PartitionConfiguration> getPartitionConfiguration(
            @PathVariable String partition) {
        try {
            CachePartitionManager.CachePartition cachePartition = CachePartitionManager.CachePartition.valueOf(partition);
            CachePartitionManager.PartitionConfiguration config = partitionManager.getPartitionConfiguration(cachePartition);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error getting partition configuration for: {}", partition, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Warm up cache for user
     */
    @PostMapping("/warming/user/{userId}")
    public ResponseEntity<String> warmUpUserCache(@PathVariable String userId) {
        try {
            cacheManager.warmUpUserCache(userId).join();
            return ResponseEntity.ok("User cache warmed up successfully");
        } catch (Exception e) {
            log.error("Error warming up user cache for: {}", userId, e);
            return ResponseEntity.internalServerError().body("Error warming up user cache: " + e.getMessage());
        }
    }

    /**
     * Warm up cache for partition
     */
    @PostMapping("/warming/partition/{partition}")
    public ResponseEntity<String> warmUpPartitionCache(@PathVariable String partition) {
        try {
            CachePartitionManager.CachePartition cachePartition = CachePartitionManager.CachePartition.valueOf(partition);
            cacheManager.warmUpPartitionCache(cachePartition).join();
            return ResponseEntity.ok("Partition cache warmed up successfully");
        } catch (Exception e) {
            log.error("Error warming up partition cache for: {}", partition, e);
            return ResponseEntity.internalServerError().body("Error warming up partition cache: " + e.getMessage());
        }
    }

    /**
     * Get warming metrics
     */
    @GetMapping("/warming/metrics")
    public ResponseEntity<IntelligentCacheManager.WarmingMetrics> getWarmingMetrics() {
        try {
            IntelligentCacheManager.WarmingMetrics metrics = cacheManager.getWarmingMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting warming metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get warming recommendations
     */
    @GetMapping("/warming/recommendations")
    public ResponseEntity<List<IntelligentCacheManager.WarmingRecommendation>> getWarmingRecommendations() {
        try {
            List<IntelligentCacheManager.WarmingRecommendation> recommendations = cacheManager.getWarmingRecommendations();
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error getting warming recommendations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get compression statistics
     */
    @GetMapping("/compression/stats")
    public ResponseEntity<CacheCompressionService.CompressionStats> getCompressionStats() {
        try {
            CacheCompressionService.CompressionStats stats = compressionService.getCompressionStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting compression statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get consistency metrics
     */
    @GetMapping("/consistency/metrics")
    public ResponseEntity<IntelligentCacheManager.ConsistencyMetrics> getConsistencyMetrics() {
        try {
            IntelligentCacheManager.ConsistencyMetrics metrics = cacheManager.getConsistencyMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting consistency metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Perform consistency check
     */
    @PostMapping("/consistency/check/{key}")
    public ResponseEntity<IntelligentCacheManager.ConsistencyCheckResult> performConsistencyCheck(
            @PathVariable String key,
            @RequestParam(defaultValue = "L2") String level) {
        try {
            IntelligentCacheManager.CacheLevel cacheLevel = IntelligentCacheManager.CacheLevel.valueOf(level);
            IntelligentCacheManager.ConsistencyCheckResult result = cacheManager.performConsistencyCheck(key, cacheLevel).join();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error performing consistency check for key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get security metrics
     */
    @GetMapping("/security/metrics")
    public ResponseEntity<CacheSecurityManager.SecurityMetrics> getSecurityMetrics() {
        try {
            CacheSecurityManager.SecurityMetrics metrics = securityManager.getSecurityMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting security metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check access permission
     */
    @GetMapping("/security/access/{key}")
    public ResponseEntity<Boolean> checkAccess(
            @PathVariable String key,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "READ") String permission) {
        try {
            CacheSecurityManager.AccessPermission accessPermission = CacheSecurityManager.AccessPermission.valueOf(permission);
            boolean hasAccess = securityManager.hasAccess(key, userId, accessPermission);
            return ResponseEntity.ok(hasAccess);
        } catch (Exception e) {
            log.error("Error checking access for key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get security audit log
     */
    @GetMapping("/security/audit/{key}")
    public ResponseEntity<List<CacheSecurityManager.SecurityEvent>> getSecurityAuditLog(@PathVariable String key) {
        try {
            List<CacheSecurityManager.SecurityEvent> auditLog = securityManager.getSecurityAuditLog(key);
            return ResponseEntity.ok(auditLog);
        } catch (Exception e) {
            log.error("Error getting security audit log for key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create user security context
     */
    @PostMapping("/security/context/{userId}")
    public ResponseEntity<String> createUserContext(
            @PathVariable String userId,
            @RequestBody Map<String, Object> contextData) {
        try {
            // Extract roles and attributes from context data
            // Handle both single role and multiple roles
            java.util.Set<String> roles;
            Object rolesData = contextData.getOrDefault("roles", java.util.List.of("USER"));
            
            if (rolesData instanceof String) {
                // Single role as string
                roles = java.util.Set.of((String) rolesData);
            } else if (rolesData instanceof java.util.List) {
                // Multiple roles as list - convert to Set
                @SuppressWarnings("unchecked")
                java.util.List<String> rolesList = (java.util.List<String>) rolesData;
                roles = new java.util.HashSet<>(rolesList);
            } else {
                // Default fallback
                roles = java.util.Set.of("USER");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) contextData.getOrDefault("attributes", Map.of());
            
            securityManager.createUserContext(userId, roles, attributes);
            return ResponseEntity.ok("User context created successfully");
        } catch (Exception e) {
            log.error("Error creating user context for: {}", userId, e);
            return ResponseEntity.internalServerError().body("Error creating user context: " + e.getMessage());
        }
    }
}
