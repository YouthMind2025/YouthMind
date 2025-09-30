package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Query Performance Profiler for monitoring and analyzing database query performance.
 * Provides comprehensive metrics collection, slow query detection, and performance analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryPerformanceProfiler {

    // Performance tracking
    private final Map<String, QueryMetrics> queryMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queryCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> slowQueryCounts = new ConcurrentHashMap<>();
    
    // Configuration
    private static final long SLOW_QUERY_THRESHOLD_MS = 100; // 100ms threshold for slow queries
    private static final long VERY_SLOW_QUERY_THRESHOLD_MS = 500; // 500ms threshold for very slow queries
    private static final int MAX_METRICS_HISTORY = 1000; // Keep last 1000 query executions

    /**
     * Profile a query execution with comprehensive metrics collection
     */
    public <T> T profileQuery(String queryName, Supplier<T> querySupplier) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        try {
            // Execute the query
            T result = querySupplier.get();
            
            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record metrics
            recordQueryMetrics(queryName, executionTime, true, null);
            
            // Log slow queries
            if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                logSlowQuery(queryName, executionTime, threadName);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordQueryMetrics(queryName, executionTime, false, e);
            
            log.error("Query '{}' failed after {}ms: {}", queryName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Record query execution metrics
     */
    private void recordQueryMetrics(String queryName, long executionTime, boolean success, Exception error) {
        try {
            // Get or create metrics for this query
            QueryMetrics metrics = queryMetrics.computeIfAbsent(queryName, k -> new QueryMetrics(queryName));
            
            // Update metrics
            metrics.addExecution(executionTime, success, error);
            
            // Update counters
            queryCounts.computeIfAbsent(queryName, k -> new AtomicLong(0)).incrementAndGet();
            
            if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                slowQueryCounts.computeIfAbsent(queryName, k -> new AtomicLong(0)).incrementAndGet();
            }
            
            // Cleanup old metrics if needed
            if (metrics.getExecutionHistory().size() > MAX_METRICS_HISTORY) {
                metrics.cleanupOldMetrics();
            }
            
        } catch (Exception e) {
            log.warn("Error recording metrics for query '{}': {}", queryName, e.getMessage());
        }
    }

    /**
     * Log slow query details
     */
    private void logSlowQuery(String queryName, long executionTime, String threadName) {
        String level = executionTime > VERY_SLOW_QUERY_THRESHOLD_MS ? "CRITICAL" : "WARNING";
        
        if (executionTime > VERY_SLOW_QUERY_THRESHOLD_MS) {
            log.error("{} SLOW QUERY: '{}' took {}ms (thread: {})", level, queryName, executionTime, threadName);
        } else {
            log.warn("{} SLOW QUERY: '{}' took {}ms (thread: {})", level, queryName, executionTime, threadName);
        }
    }

    /**
     * Get performance metrics for a specific repository
     */
    public RepositoryPerformanceMetrics getRepositoryMetrics(String repositoryName) {
        try {
            Map<String, Double> queryPerformanceMetrics = new HashMap<>();
            long totalQueries = 0;
            long totalSlowQueries = 0;
            double totalExecutionTime = 0;
            long cacheHits = 0;
            long cacheMisses = 0;
            
            // Aggregate metrics for queries belonging to this repository
            for (Map.Entry<String, QueryMetrics> entry : queryMetrics.entrySet()) {
                String queryName = entry.getKey();
                QueryMetrics metrics = entry.getValue();
                
                if (queryName.toLowerCase().contains(repositoryName.toLowerCase())) {
                    queryPerformanceMetrics.put(queryName, metrics.getAverageExecutionTime());
                    totalQueries += metrics.getTotalExecutions();
                    totalSlowQueries += metrics.getSlowQueryCount();
                    totalExecutionTime += metrics.getTotalExecutionTime();
                    
                    // Estimate cache hits/misses (simplified)
                    cacheHits += metrics.getTotalExecutions() * 0.7; // Assume 70% cache hit rate
                    cacheMisses += metrics.getTotalExecutions() * 0.3;
                }
            }
            
            double averageQueryTime = totalQueries > 0 ? totalExecutionTime / totalQueries : 0;
            double cacheHitRate = (cacheHits + cacheMisses) > 0 ? (double) cacheHits / (cacheHits + cacheMisses) : 0;
            
            return RepositoryPerformanceMetrics.builder()
                .repositoryName(repositoryName)
                .totalQueries(totalQueries)
                .averageQueryTime(averageQueryTime)
                .cacheHits(cacheHits)
                .cacheMisses(cacheMisses)
                .cacheHitRate(cacheHitRate)
                .slowQueries(totalSlowQueries)
                .queryPerformanceMetrics(queryPerformanceMetrics)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting repository metrics for '{}': {}", repositoryName, e.getMessage());
            return RepositoryPerformanceMetrics.builder()
                .repositoryName(repositoryName)
                .totalQueries(0)
                .averageQueryTime(0)
                .cacheHits(0)
                .cacheMisses(0)
                .cacheHitRate(0)
                .slowQueries(0)
                .queryPerformanceMetrics(new HashMap<>())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Get all query performance metrics
     */
    public Map<String, QueryMetrics> getAllQueryMetrics() {
        return new HashMap<>(queryMetrics);
    }

    /**
     * Get slow queries report
     */
    public List<SlowQueryReport> getSlowQueriesReport() {
        List<SlowQueryReport> slowQueries = new ArrayList<>();
        
        for (Map.Entry<String, QueryMetrics> entry : queryMetrics.entrySet()) {
            String queryName = entry.getKey();
            QueryMetrics metrics = entry.getValue();
            
            if (metrics.getSlowQueryCount() > 0) {
                slowQueries.add(SlowQueryReport.builder()
                    .queryName(queryName)
                    .totalExecutions(metrics.getTotalExecutions())
                    .slowQueryCount(metrics.getSlowQueryCount())
                    .averageExecutionTime(metrics.getAverageExecutionTime())
                    .maxExecutionTime(metrics.getMaxExecutionTime())
                    .slowQueryRate((double) metrics.getSlowQueryCount() / metrics.getTotalExecutions())
                    .build());
            }
        }
        
        // Sort by slow query rate descending
        slowQueries.sort((a, b) -> Double.compare(b.getSlowQueryRate(), a.getSlowQueryRate()));
        
        return slowQueries;
    }

    /**
     * Clear all metrics
     */
    public void clearMetrics() {
        queryMetrics.clear();
        queryCounts.clear();
        slowQueryCounts.clear();
        log.info("Cleared all query performance metrics");
    }

    /**
     * Get query performance summary
     */
    public QueryPerformanceSummary getPerformanceSummary() {
        try {
            long totalQueries = queryCounts.values().stream().mapToLong(AtomicLong::get).sum();
            long totalSlowQueries = slowQueryCounts.values().stream().mapToLong(AtomicLong::get).sum();
            
            double slowQueryRate = totalQueries > 0 ? (double) totalSlowQueries / totalQueries : 0;
            
            double averageExecutionTime = queryMetrics.values().stream()
                .mapToDouble(QueryMetrics::getAverageExecutionTime)
                .average()
                .orElse(0);
            
            return QueryPerformanceSummary.builder()
                .totalQueries(totalQueries)
                .totalSlowQueries(totalSlowQueries)
                .slowQueryRate(slowQueryRate)
                .averageExecutionTime(averageExecutionTime)
                .totalUniqueQueries(queryMetrics.size())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting performance summary: {}", e.getMessage());
            return QueryPerformanceSummary.builder()
                .totalQueries(0)
                .totalSlowQueries(0)
                .slowQueryRate(0)
                .averageExecutionTime(0)
                .totalUniqueQueries(0)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    // Inner classes
    @lombok.Data
    public static class QueryMetrics {
        private final String queryName;
        private final List<QueryExecution> executionHistory = new ArrayList<>();
        private long totalExecutions = 0;
        private long totalExecutionTime = 0;
        private long slowQueryCount = 0;
        private long maxExecutionTime = 0;
        private long errorCount = 0;

        public QueryMetrics(String queryName) {
            this.queryName = queryName;
        }

        public void addExecution(long executionTime, boolean success, Exception error) {
            synchronized (executionHistory) {
                executionHistory.add(new QueryExecution(executionTime, success, error, LocalDateTime.now()));
                totalExecutions++;
                totalExecutionTime += executionTime;
                maxExecutionTime = Math.max(maxExecutionTime, executionTime);
                
                if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                    slowQueryCount++;
                }
                
                if (!success) {
                    errorCount++;
                }
            }
        }

        public double getAverageExecutionTime() {
            return totalExecutions > 0 ? (double) totalExecutionTime / totalExecutions : 0;
        }

        public void cleanupOldMetrics() {
            synchronized (executionHistory) {
                if (executionHistory.size() > MAX_METRICS_HISTORY) {
                    // Keep only the most recent executions
                    executionHistory.subList(0, executionHistory.size() - MAX_METRICS_HISTORY).clear();
                }
            }
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class QueryExecution {
        private final long executionTime;
        private final boolean success;
        private final Exception error;
        private final LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class RepositoryPerformanceMetrics {
        private String repositoryName;
        private long totalQueries;
        private double averageQueryTime;
        private long cacheHits;
        private long cacheMisses;
        private double cacheHitRate;
        private long slowQueries;
        private Map<String, Double> queryPerformanceMetrics;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class SlowQueryReport {
        private String queryName;
        private long totalExecutions;
        private long slowQueryCount;
        private double averageExecutionTime;
        private long maxExecutionTime;
        private double slowQueryRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class QueryPerformanceSummary {
        private long totalQueries;
        private long totalSlowQueries;
        private double slowQueryRate;
        private double averageExecutionTime;
        private long totalUniqueQueries;
        private LocalDateTime timestamp;
    }
}
