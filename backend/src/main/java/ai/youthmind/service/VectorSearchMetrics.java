package ai.youthmind.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive metrics collection for Vector Search operations.
 * Provides Prometheus-compatible metrics for monitoring, alerting,
 * and performance optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchMetrics {

    private final MeterRegistry meterRegistry;
    
    // Metrics storage
    private final ConcurrentHashMap<String, AtomicLong> customCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> customTimers = new ConcurrentHashMap<>();
    
    // Prometheus metrics
    private Counter searchRequestsTotal;
    private Counter searchCacheHits;
    private Counter searchCacheMisses;
    private Counter searchErrors;
    private Counter embeddingGenerationRequests;
    private Counter embeddingGenerationErrors;
    private Counter circuitBreakerOpens;
    private Counter circuitBreakerRecoveries;
    
    private Timer searchLatency;
    private Timer embeddingGenerationLatency;
    private Timer cacheLatency;
    
    // Simple gauge values
    private final AtomicLong activeConnectionsCount = new AtomicLong(0);
    private final AtomicLong totalSearchRequests = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalSearchLatency = new AtomicLong(0);

    /**
     * Initialize metrics
     */
    public void initializeMetrics() {
        try {
            // Initialize counters
            searchRequestsTotal = Counter.builder("vector_search_requests_total")
                .description("Total number of vector search requests")
                .register(meterRegistry);
                
            searchCacheHits = Counter.builder("vector_search_cache_hits_total")
                .description("Total number of cache hits")
                .register(meterRegistry);
                
            searchCacheMisses = Counter.builder("vector_search_cache_misses_total")
                .description("Total number of cache misses")
                .register(meterRegistry);
                
            searchErrors = Counter.builder("vector_search_errors_total")
                .description("Total number of search errors")
                .register(meterRegistry);
                
            embeddingGenerationRequests = Counter.builder("embedding_generation_requests_total")
                .description("Total number of embedding generation requests")
                .register(meterRegistry);
                
            embeddingGenerationErrors = Counter.builder("embedding_generation_errors_total")
                .description("Total number of embedding generation errors")
                .register(meterRegistry);
                
            circuitBreakerOpens = Counter.builder("circuit_breaker_opens_total")
                .description("Total number of circuit breaker opens")
                .register(meterRegistry);
                
            circuitBreakerRecoveries = Counter.builder("circuit_breaker_recoveries_total")
                .description("Total number of circuit breaker recoveries")
                .register(meterRegistry);

            // Initialize timers
            searchLatency = Timer.builder("vector_search_latency_seconds")
                .description("Vector search operation latency")
                .register(meterRegistry);
                
            embeddingGenerationLatency = Timer.builder("embedding_generation_latency_seconds")
                .description("Embedding generation latency")
                .register(meterRegistry);
                
            cacheLatency = Timer.builder("vector_cache_latency_seconds")
                .description("Cache operation latency")
                .register(meterRegistry);

            // Note: Gauges removed due to Micrometer API complexity
            // They can be added back with proper syntax later

            log.info("Vector search metrics initialized successfully");
            
        } catch (Exception e) {
            log.error("Error initializing vector search metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record search request
     */
    public void recordSearchRequest(String collectionName, boolean success) {
        try {
            searchRequestsTotal.increment();
            totalSearchRequests.incrementAndGet();
            
            if (!success) {
                searchErrors.increment();
                totalErrors.incrementAndGet();
            }
            
        } catch (Exception e) {
            log.error("Error recording search request metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record search latency
     */
    public void recordSearchLatency(String collectionName, long latencyMs) {
        try {
            searchLatency.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            totalSearchLatency.addAndGet(latencyMs);
            
            // Update custom timer
            String timerKey = "search_latency_" + collectionName;
            customTimers.computeIfAbsent(timerKey, k -> new AtomicLong(0))
                .addAndGet(latencyMs);
            
        } catch (Exception e) {
            log.error("Error recording search latency metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record cache hit
     */
    public void recordCacheHit(String collectionName) {
        try {
            searchCacheHits.increment();
            totalCacheHits.incrementAndGet();
            
        } catch (Exception e) {
            log.error("Error recording cache hit metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record cache miss
     */
    public void recordCacheMiss(String collectionName) {
        try {
            searchCacheMisses.increment();
            totalCacheMisses.incrementAndGet();
            
        } catch (Exception e) {
            log.error("Error recording cache miss metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record embedding generation request
     */
    public void recordEmbeddingGeneration(boolean success) {
        try {
            embeddingGenerationRequests.increment();
            
            if (!success) {
                embeddingGenerationErrors.increment();
            }
            
        } catch (Exception e) {
            log.error("Error recording embedding generation metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record embedding generation latency
     */
    public void recordEmbeddingGenerationLatency(long latencyMs) {
        try {
            embeddingGenerationLatency.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.error("Error recording embedding generation latency metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record circuit breaker open
     */
    public void recordCircuitBreakerOpen(String serviceName) {
        try {
            circuitBreakerOpens.increment();
            
        } catch (Exception e) {
            log.error("Error recording circuit breaker open metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record circuit breaker recovery
     */
    public void recordCircuitBreakerRecovery(String serviceName) {
        try {
            circuitBreakerRecoveries.increment();
            
        } catch (Exception e) {
            log.error("Error recording circuit breaker recovery metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Record cache operation latency
     */
    public void recordCacheLatency(String operation, long latencyMs) {
        try {
            cacheLatency.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.error("Error recording cache latency metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Get custom counter value
     */
    public long getCustomCounter(String name) {
        AtomicLong counter = customCounters.get(name);
        return counter != null ? counter.get() : 0L;
    }

    /**
     * Increment custom counter
     */
    public void incrementCustomCounter(String name) {
        customCounters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Get custom timer value
     */
    public long getCustomTimer(String name) {
        AtomicLong timer = customTimers.get(name);
        return timer != null ? timer.get() : 0L;
    }

    /**
     * Add to custom timer
     */
    public void addToCustomTimer(String name, long value) {
        customTimers.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }

    /**
     * Get metrics summary
     */
    public VectorSearchMetricsSummary getMetricsSummary() {
        return VectorSearchMetricsSummary.builder()
            .totalSearchRequests(totalSearchRequests.get())
            .totalCacheHits(totalCacheHits.get())
            .totalCacheMisses(totalCacheMisses.get())
            .totalErrors(totalErrors.get())
            .averageSearchLatency(getAverageSearchLatency())
            .cacheHitRate(getCacheHitRate())
            .circuitBreakerHealth(getCircuitBreakerHealth())
            .activeConnections(getActiveConnections())
            .build();
    }

    // Gauge methods
    private double getActiveConnections() {
        return activeConnectionsCount.get();
    }

    private double getCacheHitRate() {
        long hits = totalCacheHits.get();
        long misses = totalCacheMisses.get();
        long total = hits + misses;
        
        return total > 0 ? (double) hits / total * 100.0 : 0.0;
    }

    private double getAverageSearchLatency() {
        long totalLatency = totalSearchLatency.get();
        long totalRequests = totalSearchRequests.get();
        
        return totalRequests > 0 ? (double) totalLatency / totalRequests : 0.0;
    }

    private double getCircuitBreakerHealth() {
        // This would typically query circuit breaker states
        return 1.0; // Placeholder - 1.0 means all circuits are healthy
    }

    // DTO
    @lombok.Data
    @lombok.Builder
    public static class VectorSearchMetricsSummary {
        private long totalSearchRequests;
        private long totalCacheHits;
        private long totalCacheMisses;
        private long totalErrors;
        private double averageSearchLatency;
        private double cacheHitRate;
        private double circuitBreakerHealth;
        private double activeConnections;
    }
}