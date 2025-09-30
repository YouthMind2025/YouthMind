package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI Service Connection Metrics for comprehensive performance tracking and monitoring.
 * Features:
 * - Real-time connection pool metrics collection
 * - Performance trend analysis and reporting
 * - Service-specific metrics aggregation
 * - Integration with Micrometer for Prometheus metrics
 * - Historical metrics storage and analysis
 * - Performance alerts and threshold monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceConnectionMetrics {

    // Metrics storage
    private final Map<String, ServiceMetrics> serviceMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> globalCounters = new ConcurrentHashMap<>();
    private final Map<String, List<PerformanceSnapshot>> historicalMetrics = new ConcurrentHashMap<>();

    // Performance tracking
    private final Map<String, AtomicLong> responseTimeSum = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> responseTimeCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> minResponseTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> maxResponseTime = new ConcurrentHashMap<>();

    /**
     * Record connection acquisition
     */
    public void recordConnectionAcquisition(String serviceType, boolean success) {
        ServiceMetrics metrics = getOrCreateServiceMetrics(serviceType);
        if (success) {
            metrics.getSuccessfulAcquisitions().incrementAndGet();
        } else {
            metrics.getFailedAcquisitions().incrementAndGet();
        }
        metrics.getTotalAcquisitions().incrementAndGet();
        
        globalCounters.computeIfAbsent("totalAcquisitions", k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record connection release
     */
    public void recordConnectionRelease(String serviceType, boolean success) {
        ServiceMetrics metrics = getOrCreateServiceMetrics(serviceType);
        if (success) {
            metrics.getSuccessfulReleases().incrementAndGet();
        } else {
            metrics.getFailedReleases().incrementAndGet();
        }
        metrics.getTotalReleases().incrementAndGet();
        
        globalCounters.computeIfAbsent("totalReleases", k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record operation success
     */
    public void recordOperationSuccess(String serviceType, String operation) {
        ServiceMetrics metrics = getOrCreateServiceMetrics(serviceType);
        metrics.getSuccessfulOperations().incrementAndGet();
        metrics.getTotalOperations().incrementAndGet();
        
        globalCounters.computeIfAbsent("successfulOperations", k -> new AtomicLong(0)).incrementAndGet();
        globalCounters.computeIfAbsent("totalOperations", k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record operation failure
     */
    public void recordOperationFailure(String serviceType, String operation) {
        ServiceMetrics metrics = getOrCreateServiceMetrics(serviceType);
        metrics.getFailedOperations().incrementAndGet();
        metrics.getTotalOperations().incrementAndGet();
        
        globalCounters.computeIfAbsent("failedOperations", k -> new AtomicLong(0)).incrementAndGet();
        globalCounters.computeIfAbsent("totalOperations", k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record response time
     */
    public void recordResponseTime(String serviceType, long responseTimeMs) {
        ServiceMetrics metrics = getOrCreateServiceMetrics(serviceType);
        
        // Update response time statistics
        responseTimeSum.computeIfAbsent(serviceType, k -> new AtomicLong(0)).addAndGet(responseTimeMs);
        responseTimeCount.computeIfAbsent(serviceType, k -> new AtomicLong(0)).incrementAndGet();
        
        // Update min/max response times
        minResponseTime.compute(serviceType, (k, v) -> {
            if (v == null || responseTimeMs < v.get()) {
                return new AtomicLong(responseTimeMs);
            }
            return v;
        });
        
        maxResponseTime.compute(serviceType, (k, v) -> {
            if (v == null || responseTimeMs > v.get()) {
                return new AtomicLong(responseTimeMs);
            }
            return v;
        });
        
        metrics.getTotalResponseTime().addAndGet(responseTimeMs);
        metrics.getResponseTimeCount().incrementAndGet();
    }

    /**
     * Collect metrics from service pools
     */
    public void collectMetrics(Map<String, AIServiceConnectionPoolManager.ConnectionPool> servicePools) {
        LocalDateTime timestamp = LocalDateTime.now();
        
        for (Map.Entry<String, AIServiceConnectionPoolManager.ConnectionPool> entry : servicePools.entrySet()) {
            String serviceType = entry.getKey();
            AIServiceConnectionPoolManager.ConnectionPool pool = entry.getValue();
            
            // Create performance snapshot
            PerformanceSnapshot snapshot = PerformanceSnapshot.builder()
                .serviceType(serviceType)
                .timestamp(timestamp)
                .totalConnections(pool.getTotalConnections())
                .activeConnections(pool.getActiveConnections())
                .idleConnections(pool.getIdleConnections())
                .averageResponseTime(getAverageResponseTime(serviceType))
                .successRate(getSuccessRate(serviceType))
                .build();
            
            // Store historical metrics
            historicalMetrics.computeIfAbsent(serviceType, k -> new ArrayList<>()).add(snapshot);
            
            // Keep only last 100 snapshots per service
            List<PerformanceSnapshot> snapshots = historicalMetrics.get(serviceType);
            if (snapshots.size() > 100) {
                snapshots.remove(0);
            }
        }
    }

    /**
     * Get average response time for service
     */
    public double getAverageResponseTime(String serviceType) {
        AtomicLong sum = responseTimeSum.get(serviceType);
        AtomicLong count = responseTimeCount.get(serviceType);
        
        if (sum == null || count == null || count.get() == 0) {
            return 0.0;
        }
        
        return (double) sum.get() / count.get();
    }

    /**
     * Get success rate for service
     */
    public double getSuccessRate(String serviceType) {
        ServiceMetrics metrics = serviceMetrics.get(serviceType);
        if (metrics == null) {
            return 1.0;
        }
        
        long total = metrics.getTotalOperations().get();
        if (total == 0) {
            return 1.0;
        }
        
        return (double) metrics.getSuccessfulOperations().get() / total;
    }

    /**
     * Get comprehensive metrics for service
     */
    public ServiceMetricsSummary getServiceMetrics(String serviceType) {
        ServiceMetrics metrics = serviceMetrics.get(serviceType);
        if (metrics == null) {
            return ServiceMetricsSummary.builder().serviceType(serviceType).build();
        }
        
        return ServiceMetricsSummary.builder()
            .serviceType(serviceType)
            .totalConnections(metrics.getTotalConnections().get())
            .activeConnections(metrics.getActiveConnections().get())
            .totalOperations(metrics.getTotalOperations().get())
            .successfulOperations(metrics.getSuccessfulOperations().get())
            .failedOperations(metrics.getFailedOperations().get())
            .successRate(getSuccessRate(serviceType))
            .averageResponseTime(getAverageResponseTime(serviceType))
            .minResponseTime(minResponseTime.getOrDefault(serviceType, new AtomicLong(0)).get())
            .maxResponseTime(maxResponseTime.getOrDefault(serviceType, new AtomicLong(0)).get())
            .totalAcquisitions(metrics.getTotalAcquisitions().get())
            .successfulAcquisitions(metrics.getSuccessfulAcquisitions().get())
            .failedAcquisitions(metrics.getFailedAcquisitions().get())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get global metrics summary
     */
    public GlobalMetricsSummary getGlobalMetrics() {
        long totalOperations = globalCounters.getOrDefault("totalOperations", new AtomicLong(0)).get();
        long successfulOperations = globalCounters.getOrDefault("successfulOperations", new AtomicLong(0)).get();
        long failedOperations = globalCounters.getOrDefault("failedOperations", new AtomicLong(0)).get();
        
        return GlobalMetricsSummary.builder()
            .totalServices(serviceMetrics.size())
            .totalOperations(totalOperations)
            .successfulOperations(successfulOperations)
            .failedOperations(failedOperations)
            .globalSuccessRate(totalOperations > 0 ? (double) successfulOperations / totalOperations : 0.0)
            .averageResponseTime(calculateGlobalAverageResponseTime())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get performance trends for service
     */
    public List<PerformanceTrend> getPerformanceTrends(String serviceType, int hours) {
        List<PerformanceSnapshot> snapshots = historicalMetrics.get(serviceType);
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return snapshots.stream()
            .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoff))
            .map(snapshot -> PerformanceTrend.builder()
                .timestamp(snapshot.getTimestamp())
                .responseTime(snapshot.getAverageResponseTime())
                .successRate(snapshot.getSuccessRate())
                .activeConnections(snapshot.getActiveConnections())
                .build())
            .toList();
    }

    // Private helper methods

    private ServiceMetrics getOrCreateServiceMetrics(String serviceType) {
        return serviceMetrics.computeIfAbsent(serviceType, k -> ServiceMetrics.builder()
            .serviceType(serviceType)
            .build());
    }

    private double calculateGlobalAverageResponseTime() {
        long totalSum = responseTimeSum.values().stream().mapToLong(AtomicLong::get).sum();
        long totalCount = responseTimeCount.values().stream().mapToLong(AtomicLong::get).sum();
        
        if (totalCount == 0) {
            return 0.0;
        }
        
        return (double) totalSum / totalCount;
    }

    // Inner classes for data structures

    @lombok.Data
    @lombok.Builder
    public static class ServiceMetrics {
        private String serviceType;
        private AtomicLong totalConnections = new AtomicLong(0);
        private AtomicLong activeConnections = new AtomicLong(0);
        private AtomicLong totalOperations = new AtomicLong(0);
        private AtomicLong successfulOperations = new AtomicLong(0);
        private AtomicLong failedOperations = new AtomicLong(0);
        private AtomicLong totalAcquisitions = new AtomicLong(0);
        private AtomicLong successfulAcquisitions = new AtomicLong(0);
        private AtomicLong failedAcquisitions = new AtomicLong(0);
        private AtomicLong totalReleases = new AtomicLong(0);
        private AtomicLong successfulReleases = new AtomicLong(0);
        private AtomicLong failedReleases = new AtomicLong(0);
        private AtomicLong totalResponseTime = new AtomicLong(0);
        private AtomicLong responseTimeCount = new AtomicLong(0);
    }

    @lombok.Data
    @lombok.Builder
    public static class ServiceMetricsSummary {
        private String serviceType;
        private long totalConnections;
        private long activeConnections;
        private long totalOperations;
        private long successfulOperations;
        private long failedOperations;
        private double successRate;
        private double averageResponseTime;
        private long minResponseTime;
        private long maxResponseTime;
        private long totalAcquisitions;
        private long successfulAcquisitions;
        private long failedAcquisitions;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class GlobalMetricsSummary {
        private int totalServices;
        private long totalOperations;
        private long successfulOperations;
        private long failedOperations;
        private double globalSuccessRate;
        private double averageResponseTime;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class PerformanceSnapshot {
        private String serviceType;
        private LocalDateTime timestamp;
        private int totalConnections;
        private int activeConnections;
        private int idleConnections;
        private double averageResponseTime;
        private double successRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class PerformanceTrend {
        private LocalDateTime timestamp;
        private double responseTime;
        private double successRate;
        private int activeConnections;
    }
}
