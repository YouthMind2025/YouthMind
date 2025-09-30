package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import ai.youthmind.util.CircuitBreaker;

/**
 * Advanced Bulkhead Pattern implementation for AI service failure isolation.
 * Features:
 * - Resource isolation between different AI services
 * - Configurable concurrency limits per service
 * - Queue management for overloaded services
 * - Failure isolation to prevent cascading failures
 * - Resource monitoring and metrics
 * - Dynamic resource allocation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceBulkhead {

    // Bulkhead configurations for different services
    private final Map<String, BulkheadConfig> serviceConfigs = new ConcurrentHashMap<>();
    
    // Semaphores for concurrency control
    private final Map<String, Semaphore> serviceSemaphores = new ConcurrentHashMap<>();
    
    // Bulkhead metrics
    private final Map<String, BulkheadMetrics> bulkheadMetrics = new ConcurrentHashMap<>();

    /**
     * Execute operation with bulkhead protection
     */
    public <T> CompletableFuture<T> executeWithBulkhead(
            String serviceName,
            String operationType,
            Supplier<T> operation) {
        
        return executeWithBulkhead(serviceName, operationType, operation, null);
    }

    /**
     * Execute operation with bulkhead protection and custom timeout
     */
    public <T> CompletableFuture<T> executeWithBulkhead(
            String serviceName,
            String operationType,
            Supplier<T> operation,
            Long customTimeoutMs) {
        
        return CompletableFuture.supplyAsync(() -> {
            BulkheadConfig config = getBulkheadConfig(serviceName);
            Semaphore semaphore = getOrCreateSemaphore(serviceName, config);
            
            try {
                // Try to acquire permit with timeout
                long timeout = customTimeoutMs != null ? customTimeoutMs : config.getAcquireTimeoutMs();
                boolean acquired = semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
                
                if (!acquired) {
                    log.warn("Bulkhead timeout for service '{}', operation '{}'", serviceName, operationType);
                    recordBulkheadTimeout(serviceName, operationType);
                    throw new RuntimeException("Bulkhead timeout: unable to acquire resource");
                }
                
                try {
                    log.debug("Executing operation '{}' for service '{}' with bulkhead protection", 
                            operationType, serviceName);
                    
                    T result = operation.get();
                    
                    // Record successful execution
                    recordSuccessfulExecution(serviceName, operationType);
                    
                    return result;
                    
                } finally {
                    // Always release the permit
                    semaphore.release();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Bulkhead operation interrupted for service '{}'", serviceName);
                recordBulkheadInterruption(serviceName, operationType);
                throw new RuntimeException("Bulkhead operation interrupted", e);
                
            } catch (Exception e) {
                log.error("Bulkhead operation failed for service '{}': {}", serviceName, e.getMessage(), e);
                recordBulkheadFailure(serviceName, operationType, e.getMessage());
                throw new RuntimeException("Bulkhead operation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Execute operation with bulkhead and fallback
     */
    public <T> CompletableFuture<T> executeWithBulkheadAndFallback(
            String serviceName,
            String operationType,
            Supplier<T> operation,
            Supplier<T> fallback) {
        
        return executeWithBulkhead(serviceName, operationType, operation)
            .exceptionally(throwable -> {
                log.warn("Bulkhead operation '{}' for service '{}' failed, executing fallback", 
                        operationType, serviceName);
                
                try {
                    return fallback.get();
                } catch (Exception e) {
                    log.error("Fallback operation also failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Both bulkhead operation and fallback failed", e);
                }
            });
    }

    /**
     * Execute operation with bulkhead and circuit breaker integration
     */
    public <T> CompletableFuture<T> executeWithBulkheadAndCircuitBreaker(
            String serviceName,
            String operationType,
            Supplier<T> operation,
            CircuitBreaker circuitBreaker,
            Supplier<T> fallback) {
        
        return circuitBreaker.executeWithCircuitBreaker(
            serviceName,
            () -> executeWithBulkhead(serviceName, operationType, operation).join(),
            () -> fallback.get()
        );
    }

    /**
     * Get bulkhead configuration for service
     */
    private BulkheadConfig getBulkheadConfig(String serviceName) {
        return serviceConfigs.computeIfAbsent(serviceName, name -> {
            // Create default configuration based on service type
            return createDefaultBulkheadConfig(name);
        });
    }

    /**
     * Create default bulkhead configuration
     */
    private BulkheadConfig createDefaultBulkheadConfig(String serviceName) {
        int maxConcurrency = switch (serviceName.toLowerCase()) {
            case "ai_analysis" -> 10;
            case "vector_search" -> 20;
            case "moderation" -> 15;
            case "psychological_analysis" -> 8;
            case "image_processing" -> 5;
            case "text_processing" -> 25;
            default -> 10;
        };
        
        return BulkheadConfig.builder()
            .serviceName(serviceName)
            .maxConcurrency(maxConcurrency)
            .acquireTimeoutMs(5000L)
            .enableMetrics(true)
            .build();
    }

    /**
     * Get or create semaphore for service
     */
    private Semaphore getOrCreateSemaphore(String serviceName, BulkheadConfig config) {
        return serviceSemaphores.computeIfAbsent(serviceName, name -> {
            log.info("Creating bulkhead semaphore for service '{}' with max concurrency {}", 
                    name, config.getMaxConcurrency());
            return new Semaphore(config.getMaxConcurrency());
        });
    }

    /**
     * Record successful execution
     */
    private void recordSuccessfulExecution(String serviceName, String operationType) {
        BulkheadMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementSuccessfulExecutions();
    }

    /**
     * Record bulkhead timeout
     */
    private void recordBulkheadTimeout(String serviceName, String operationType) {
        BulkheadMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementTimeouts();
    }

    /**
     * Record bulkhead interruption
     */
    private void recordBulkheadInterruption(String serviceName, String operationType) {
        BulkheadMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementInterruptions();
    }

    /**
     * Record bulkhead failure
     */
    private void recordBulkheadFailure(String serviceName, String operationType, String errorMessage) {
        BulkheadMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementFailures();
        metrics.recordError(errorMessage);
    }

    /**
     * Get or create metrics for service
     */
    private BulkheadMetrics getOrCreateMetrics(String serviceName) {
        return bulkheadMetrics.computeIfAbsent(serviceName, name -> new BulkheadMetrics(name));
    }

    /**
     * Get current bulkhead status for service
     */
    public BulkheadStatus getBulkheadStatus(String serviceName) {
        BulkheadConfig config = serviceConfigs.get(serviceName);
        Semaphore semaphore = serviceSemaphores.get(serviceName);
        BulkheadMetrics metrics = bulkheadMetrics.get(serviceName);
        
        if (config == null || semaphore == null) {
            return BulkheadStatus.builder()
                .serviceName(serviceName)
                .isActive(false)
                .availablePermits(0)
                .maxConcurrency(0)
                .build();
        }
        
        return BulkheadStatus.builder()
            .serviceName(serviceName)
            .isActive(true)
            .availablePermits(semaphore.availablePermits())
            .maxConcurrency(config.getMaxConcurrency())
            .queueLength(config.getMaxConcurrency() - semaphore.availablePermits())
            .successRate(metrics != null ? metrics.getSuccessRate() : 0.0)
            .timeoutRate(metrics != null ? metrics.getTimeoutRate() : 0.0)
            .build();
    }

    /**
     * Get bulkhead metrics for service
     */
    public BulkheadMetrics getBulkheadMetrics(String serviceName) {
        return bulkheadMetrics.get(serviceName);
    }

    /**
     * Update bulkhead configuration
     */
    public void updateBulkheadConfig(String serviceName, BulkheadConfig config) {
        serviceConfigs.put(serviceName, config);
        
        // Recreate semaphore with new configuration
        Semaphore oldSemaphore = serviceSemaphores.remove(serviceName);
        if (oldSemaphore != null) {
            // Drain remaining permits
            oldSemaphore.drainPermits();
        }
        
        // Create new semaphore
        getOrCreateSemaphore(serviceName, config);
        
        log.info("Updated bulkhead configuration for service '{}' with max concurrency {}", 
                serviceName, config.getMaxConcurrency());
    }

    /**
     * Get all bulkhead statuses
     */
    public Map<String, BulkheadStatus> getAllBulkheadStatuses() {
        Map<String, BulkheadStatus> statuses = new ConcurrentHashMap<>();
        
        for (String serviceName : serviceConfigs.keySet()) {
            statuses.put(serviceName, getBulkheadStatus(serviceName));
        }
        
        return statuses;
    }

    /**
     * Get overall bulkhead statistics
     */
    public BulkheadStatistics getOverallBulkheadStatistics() {
        long totalExecutions = bulkheadMetrics.values().stream()
            .mapToLong(BulkheadMetrics::getTotalExecutions)
            .sum();
        
        long totalTimeouts = bulkheadMetrics.values().stream()
            .mapToLong(BulkheadMetrics::getTimeoutCount)
            .sum();
        
        long totalFailures = bulkheadMetrics.values().stream()
            .mapToLong(BulkheadMetrics::getFailureCount)
            .sum();
        
        double overallTimeoutRate = totalExecutions > 0 ? (double) totalTimeouts / totalExecutions : 0.0;
        double overallFailureRate = totalExecutions > 0 ? (double) totalFailures / totalExecutions : 0.0;
        
        return BulkheadStatistics.builder()
            .totalExecutions(totalExecutions)
            .totalTimeouts(totalTimeouts)
            .totalFailures(totalFailures)
            .overallTimeoutRate(overallTimeoutRate)
            .overallFailureRate(overallFailureRate)
            .serviceMetrics(bulkheadMetrics)
            .build();
    }

    /**
     * Clear bulkhead metrics for service
     */
    public void clearBulkheadMetrics(String serviceName) {
        bulkheadMetrics.remove(serviceName);
        log.info("Cleared bulkhead metrics for service '{}'", serviceName);
    }

    /**
     * Reset bulkhead for service (recreate semaphore)
     */
    public void resetBulkhead(String serviceName) {
        Semaphore oldSemaphore = serviceSemaphores.remove(serviceName);
        if (oldSemaphore != null) {
            oldSemaphore.drainPermits();
        }
        
        BulkheadConfig config = serviceConfigs.get(serviceName);
        if (config != null) {
            getOrCreateSemaphore(serviceName, config);
            log.info("Reset bulkhead for service '{}'", serviceName);
        }
    }

    // Configuration and metrics classes
    @lombok.Data
    @lombok.Builder
    public static class BulkheadConfig {
        private String serviceName;
        private int maxConcurrency;
        private long acquireTimeoutMs;
        private boolean enableMetrics;
        private Map<String, Object> customConfig;
    }

    @lombok.Data
    @lombok.Builder
    public static class BulkheadStatus {
        private String serviceName;
        private boolean isActive;
        private int availablePermits;
        private int maxConcurrency;
        private int queueLength;
        private double successRate;
        private double timeoutRate;
    }

    @lombok.Data
    public static class BulkheadMetrics {
        private final String serviceName;
        private long successfulExecutions = 0;
        private long timeoutCount = 0;
        private long failureCount = 0;
        private long interruptionCount = 0;
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public BulkheadMetrics(String serviceName) {
            this.serviceName = serviceName;
        }

        public void incrementSuccessfulExecutions() {
            successfulExecutions++;
        }

        public void incrementTimeouts() {
            timeoutCount++;
        }

        public void incrementFailures() {
            failureCount++;
        }

        public void incrementInterruptions() {
            interruptionCount++;
        }

        public void recordError(String error) {
            errors.add(error);
            
            // Keep only last 50 errors
            if (errors.size() > 50) {
                errors.remove(0);
            }
        }

        public long getTotalExecutions() {
            return successfulExecutions + timeoutCount + failureCount + interruptionCount;
        }

        public double getSuccessRate() {
            long total = getTotalExecutions();
            return total > 0 ? (double) successfulExecutions / total : 0.0;
        }

        public double getTimeoutRate() {
            long total = getTotalExecutions();
            return total > 0 ? (double) timeoutCount / total : 0.0;
        }

        public double getFailureRate() {
            long total = getTotalExecutions();
            return total > 0 ? (double) failureCount / total : 0.0;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class BulkheadStatistics {
        private long totalExecutions;
        private long totalTimeouts;
        private long totalFailures;
        private double overallTimeoutRate;
        private double overallFailureRate;
        private Map<String, BulkheadMetrics> serviceMetrics;
    }
}
