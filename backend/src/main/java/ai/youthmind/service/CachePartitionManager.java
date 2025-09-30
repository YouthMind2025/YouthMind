package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache Partition Manager for intelligent data type separation and optimization.
 * Features:
 * - Data type detection and automatic partitioning
 * - Partition-specific optimization strategies
 * - Access pattern analysis and optimization
 * - Partition statistics and monitoring
 * - Dynamic partition configuration
 * - Cache level optimization per partition
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachePartitionManager {

    // Cache partitions
    public enum CachePartition {
        PSYCHOLOGICAL_STATE("psychological_state:", 0.8, 30, 120),
        USER_PROFILE("user_profile:", 0.9, 60, 240),
        POST_CONTENT("post_content:", 0.7, 15, 60),
        VECTOR_SEARCH("vector_search:", 0.6, 5, 30),
        AI_RESPONSES("ai_responses:", 0.5, 10, 45),
        SESSION_DATA("session_data:", 0.8, 20, 90),
        FEED_DATA("feed_data:", 0.6, 5, 20),
        MODERATION_RESULTS("moderation_results:", 0.7, 15, 60),
        ANALYTICS_DATA("analytics_data:", 0.4, 30, 180),
        SYSTEM_CONFIG("system_config:", 0.95, 120, 480);

        private final String keyPrefix;
        private final double hitRateThreshold;
        private final int l1TtlMinutes;
        private final int l2TtlMinutes;

        CachePartition(String keyPrefix, double hitRateThreshold, int l1TtlMinutes, int l2TtlMinutes) {
            this.keyPrefix = keyPrefix;
            this.hitRateThreshold = hitRateThreshold;
            this.l1TtlMinutes = l1TtlMinutes;
            this.l2TtlMinutes = l2TtlMinutes;
        }

        public String getKeyPrefix() { return keyPrefix; }
        public double getHitRateThreshold() { return hitRateThreshold; }
        public int getL1TtlMinutes() { return l1TtlMinutes; }
        public int getL2TtlMinutes() { return l2TtlMinutes; }
    }

    // Partition statistics
    private final Map<CachePartition, PartitionStats> partitionStats = new ConcurrentHashMap<>();
    private final Map<String, CachePartition> keyToPartition = new ConcurrentHashMap<>();
    private final Map<CachePartition, AtomicLong> partitionHits = new ConcurrentHashMap<>();
    private final Map<CachePartition, AtomicLong> partitionMisses = new ConcurrentHashMap<>();

    /**
     * Initialize cache partitions
     */
    public void initializePartitions() {
        log.info("Initializing cache partitions");
        
        for (CachePartition partition : CachePartition.values()) {
            partitionStats.put(partition, PartitionStats.builder()
                .partition(partition)
                .hitCount(0)
                .missCount(0)
                .totalRequests(0)
                .hitRate(0.0)
                .averageAccessTime(0.0)
                .lastUpdated(LocalDateTime.now())
                .build());
                
            partitionHits.put(partition, new AtomicLong(0));
            partitionMisses.put(partition, new AtomicLong(0));
        }
        
        log.info("Initialized {} cache partitions", CachePartition.values().length);
    }

    /**
     * Get partition for cache key
     */
    public CachePartition getPartition(String key) {
        // Check if already mapped
        CachePartition cachedPartition = keyToPartition.get(key);
        if (cachedPartition != null) {
            return cachedPartition;
        }

        // Determine partition based on key prefix
        CachePartition partition = determinePartitionByKey(key);
        keyToPartition.put(key, partition);
        
        return partition;
    }

    /**
     * Determine optimal cache level for partition
     */
    public IntelligentCacheManager.CacheLevel determineOptimalLevel(CachePartition partition, String key) {
        PartitionStats stats = partitionStats.get(partition);
        if (stats == null) {
            return IntelligentCacheManager.CacheLevel.L2; // Default
        }

        double hitRate = stats.getHitRate();
        
        // High hit rate -> L1 cache
        if (hitRate >= partition.getHitRateThreshold()) {
            return IntelligentCacheManager.CacheLevel.L1;
        }
        
        // Medium hit rate -> L2 cache
        if (hitRate >= partition.getHitRateThreshold() * 0.7) {
            return IntelligentCacheManager.CacheLevel.L2;
        }
        
        // Low hit rate -> L3 cache
        return IntelligentCacheManager.CacheLevel.L3;
    }

    /**
     * Get TTL for partition and cache level
     */
    public int getTtlMinutes(CachePartition partition, IntelligentCacheManager.CacheLevel level) {
        return switch (level) {
            case L1 -> partition.getL1TtlMinutes();
            case L2 -> partition.getL2TtlMinutes();
            case L3 -> partition.getL2TtlMinutes() * 2; // L3 typically longer
            default -> partition.getL2TtlMinutes();
        };
    }

    /**
     * Record cache hit for partition
     */
    public void recordHit(CachePartition partition) {
        partitionHits.get(partition).incrementAndGet();
        updatePartitionStats(partition);
    }

    /**
     * Record cache miss for partition
     */
    public void recordMiss(CachePartition partition) {
        partitionMisses.get(partition).incrementAndGet();
        updatePartitionStats(partition);
    }

    /**
     * Get partition statistics
     */
    public Map<CachePartition, PartitionStats> getPartitionStatistics() {
        return new HashMap<>(partitionStats);
    }

    /**
     * Get partition performance summary
     */
    public PartitionPerformanceSummary getPerformanceSummary() {
        Map<CachePartition, Double> hitRates = new HashMap<>();
        Map<CachePartition, Long> totalRequests = new HashMap<>();
        
        for (Map.Entry<CachePartition, PartitionStats> entry : partitionStats.entrySet()) {
            CachePartition partition = entry.getKey();
            PartitionStats stats = entry.getValue();
            
            hitRates.put(partition, stats.getHitRate());
            totalRequests.put(partition, stats.getTotalRequests());
        }
        
        return PartitionPerformanceSummary.builder()
            .partitionHitRates(hitRates)
            .partitionTotalRequests(totalRequests)
            .overallHitRate(calculateOverallHitRate())
            .bestPerformingPartition(findBestPerformingPartition())
            .worstPerformingPartition(findWorstPerformingPartition())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Update partition statistics
     */
    public void updateStatistics() {
        for (CachePartition partition : CachePartition.values()) {
            updatePartitionStats(partition);
        }
    }

    /**
     * Get partition configuration
     */
    public PartitionConfiguration getPartitionConfiguration(CachePartition partition) {
        return PartitionConfiguration.builder()
            .partition(partition)
            .keyPrefix(partition.getKeyPrefix())
            .hitRateThreshold(partition.getHitRateThreshold())
            .l1TtlMinutes(partition.getL1TtlMinutes())
            .l2TtlMinutes(partition.getL2TtlMinutes())
            .currentHitRate(getCurrentHitRate(partition))
            .totalRequests(getTotalRequests(partition))
            .build();
    }

    // Private helper methods

    private CachePartition determinePartitionByKey(String key) {
        for (CachePartition partition : CachePartition.values()) {
            if (key.startsWith(partition.getKeyPrefix())) {
                return partition;
            }
        }
        
        // Default partition for unknown keys
        return CachePartition.SYSTEM_CONFIG;
    }

    private void updatePartitionStats(CachePartition partition) {
        long hits = partitionHits.get(partition).get();
        long misses = partitionMisses.get(partition).get();
        long totalRequests = hits + misses;
        
        double hitRate = totalRequests > 0 ? (double) hits / totalRequests : 0.0;
        
        PartitionStats stats = partitionStats.get(partition);
        if (stats != null) {
            stats.setHitCount(hits);
            stats.setMissCount(misses);
            stats.setTotalRequests(totalRequests);
            stats.setHitRate(hitRate);
            stats.setLastUpdated(LocalDateTime.now());
        }
    }

    private double calculateOverallHitRate() {
        long totalHits = partitionHits.values().stream().mapToLong(AtomicLong::get).sum();
        long totalMisses = partitionMisses.values().stream().mapToLong(AtomicLong::get).sum();
        long totalRequests = totalHits + totalMisses;
        
        return totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
    }

    private CachePartition findBestPerformingPartition() {
        return partitionStats.entrySet().stream()
            .max(Map.Entry.comparingByValue(Comparator.comparing(PartitionStats::getHitRate)))
            .map(Map.Entry::getKey)
            .orElse(CachePartition.SYSTEM_CONFIG);
    }

    private CachePartition findWorstPerformingPartition() {
        return partitionStats.entrySet().stream()
            .min(Map.Entry.comparingByValue(Comparator.comparing(PartitionStats::getHitRate)))
            .map(Map.Entry::getKey)
            .orElse(CachePartition.ANALYTICS_DATA);
    }

    private double getCurrentHitRate(CachePartition partition) {
        PartitionStats stats = partitionStats.get(partition);
        return stats != null ? stats.getHitRate() : 0.0;
    }

    private long getTotalRequests(CachePartition partition) {
        PartitionStats stats = partitionStats.get(partition);
        return stats != null ? stats.getTotalRequests() : 0;
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class PartitionStats {
        private CachePartition partition;
        private long hitCount;
        private long missCount;
        private long totalRequests;
        private double hitRate;
        private double averageAccessTime;
        private LocalDateTime lastUpdated;
    }

    @lombok.Data
    @lombok.Builder
    public static class PartitionPerformanceSummary {
        private Map<CachePartition, Double> partitionHitRates;
        private Map<CachePartition, Long> partitionTotalRequests;
        private double overallHitRate;
        private CachePartition bestPerformingPartition;
        private CachePartition worstPerformingPartition;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class PartitionConfiguration {
        private CachePartition partition;
        private String keyPrefix;
        private double hitRateThreshold;
        private int l1TtlMinutes;
        private int l2TtlMinutes;
        private double currentHitRate;
        private long totalRequests;
    }
}
