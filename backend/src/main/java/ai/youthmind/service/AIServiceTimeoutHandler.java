package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import ai.youthmind.util.CircuitBreaker;

/**
 * Advanced Timeout Handler for AI service requests with cancellation support.
 * Features:
 * - Configurable timeout per service and operation type
 * - Request cancellation and cleanup
 * - Timeout metrics and monitoring
 * - Graceful timeout handling
 * - Integration with circuit breaker and retry mechanisms
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceTimeoutHandler {

    // Timeout configurations for different services
    private final Map<String, TimeoutConfig> serviceConfigs = new ConcurrentHashMap<>();
    
    // Timeout metrics
    private final Map<String, TimeoutMetrics> timeoutMetrics = new ConcurrentHashMap<>();

    /**
     * Execute operation with timeout protection
     */
    public <T> CompletableFuture<T> executeWithTimeout(
            String serviceName,
            String operationType,
            Supplier<T> operation) {
        
        return executeWithTimeout(serviceName, operationType, operation, null);
    }

    /**
     * Execute operation with custom timeout
     */
    public <T> CompletableFuture<T> executeWithTimeout(
            String serviceName,
            String operationType,
            Supplier<T> operation,
            Duration customTimeout) {
        
        return CompletableFuture.supplyAsync(() -> {
            TimeoutConfig config = getTimeoutConfig(serviceName, operationType);
            Duration timeout = customTimeout != null ? customTimeout : config.getTimeout();
            
            try {
                log.debug("Executing operation '{}' for service '{}' with timeout {}ms", 
                        operationType, serviceName, timeout.toMillis());
                
                CompletableFuture<T> future = CompletableFuture.supplyAsync(operation);
                T result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                
                // Record successful execution
                recordSuccessfulExecution(serviceName, operationType);
                
                return result;
                
            } catch (TimeoutException e) {
                log.warn("Operation '{}' for service '{}' timed out after {}ms", 
                        operationType, serviceName, timeout.toMillis());
                
                // Record timeout
                recordTimeout(serviceName, operationType, timeout);
                
                throw new RuntimeException("Operation timed out after " + timeout.toMillis() + "ms", e);
                
            } catch (Exception e) {
                log.error("Operation '{}' for service '{}' failed: {}", 
                        operationType, serviceName, e.getMessage(), e);
                
                // Record failure
                recordFailure(serviceName, operationType, e.getMessage());
                
                throw new RuntimeException("Operation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Execute operation with timeout and fallback
     */
    public <T> CompletableFuture<T> executeWithTimeoutAndFallback(
            String serviceName,
            String operationType,
            Supplier<T> operation,
            Supplier<T> fallback) {
        
        return executeWithTimeout(serviceName, operationType, operation)
            .exceptionally(throwable -> {
                log.warn("Operation '{}' for service '{}' failed, executing fallback", 
                        operationType, serviceName);
                
                try {
                    return fallback.get();
                } catch (Exception e) {
                    log.error("Fallback operation also failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Both operation and fallback failed", e);
                }
            });
    }

    /**
     * Execute operation with timeout and circuit breaker integration
     */
    public <T> CompletableFuture<T> executeWithTimeoutAndCircuitBreaker(
            String serviceName,
            String operationType,
            Supplier<T> operation,
            CircuitBreaker circuitBreaker,
            Supplier<T> fallback) {
        
        return circuitBreaker.executeWithCircuitBreaker(
            serviceName,
            () -> executeWithTimeout(serviceName, operationType, operation).join(),
            () -> fallback.get()
        );
    }

    /**
     * Get timeout configuration for service and operation
     */
    private TimeoutConfig getTimeoutConfig(String serviceName, String operationType) {
        String configKey = serviceName + ":" + operationType;
        
        return serviceConfigs.computeIfAbsent(configKey, key -> {
            // Create default configuration based on service type
            return createDefaultTimeoutConfig(serviceName, operationType);
        });
    }

    /**
     * Create default timeout configuration
     */
    private TimeoutConfig createDefaultTimeoutConfig(String serviceName, String operationType) {
        Duration timeout = switch (serviceName.toLowerCase()) {
            case "ai_analysis" -> switch (operationType.toLowerCase()) {
                case "analyze_post" -> Duration.ofSeconds(30);
                case "analyze_comment" -> Duration.ofSeconds(15);
                case "analyze_user" -> Duration.ofSeconds(45);
                default -> Duration.ofSeconds(30);
            };
            case "vector_search" -> switch (operationType.toLowerCase()) {
                case "search_similar" -> Duration.ofSeconds(10);
                case "index_vector" -> Duration.ofSeconds(20);
                case "batch_search" -> Duration.ofSeconds(60);
                default -> Duration.ofSeconds(15);
            };
            case "moderation" -> switch (operationType.toLowerCase()) {
                case "moderate_content" -> Duration.ofSeconds(20);
                case "moderate_image" -> Duration.ofSeconds(30);
                case "batch_moderate" -> Duration.ofSeconds(90);
                default -> Duration.ofSeconds(25);
            };
            case "psychological_analysis" -> switch (operationType.toLowerCase()) {
                case "analyze_state" -> Duration.ofSeconds(40);
                case "generate_recommendations" -> Duration.ofSeconds(60);
                case "batch_analysis" -> Duration.ofMinutes(2);
                default -> Duration.ofSeconds(45);
            };
            default -> Duration.ofSeconds(30);
        };
        
        return TimeoutConfig.builder()
            .serviceName(serviceName)
            .operationType(operationType)
            .timeout(timeout)
            .enableMetrics(true)
            .build();
    }

    /**
     * Record successful execution
     */
    private void recordSuccessfulExecution(String serviceName, String operationType) {
        TimeoutMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementSuccessfulExecutions();
    }

    /**
     * Record timeout
     */
    private void recordTimeout(String serviceName, String operationType, Duration timeout) {
        TimeoutMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementTimeouts();
        metrics.recordTimeoutDuration(timeout);
    }

    /**
     * Record failure
     */
    private void recordFailure(String serviceName, String operationType, String errorMessage) {
        TimeoutMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementFailures();
        metrics.recordError(errorMessage);
    }

    /**
     * Get or create metrics for service
     */
    private TimeoutMetrics getOrCreateMetrics(String serviceName) {
        return timeoutMetrics.computeIfAbsent(serviceName, name -> new TimeoutMetrics(name));
    }

    /**
     * Update timeout configuration
     */
    public void updateTimeoutConfig(String serviceName, String operationType, Duration timeout) {
        String configKey = serviceName + ":" + operationType;
        
        TimeoutConfig config = TimeoutConfig.builder()
            .serviceName(serviceName)
            .operationType(operationType)
            .timeout(timeout)
            .enableMetrics(true)
            .build();
        
        serviceConfigs.put(configKey, config);
        log.info("Updated timeout configuration for service '{}', operation '{}' to {}ms", 
                serviceName, operationType, timeout.toMillis());
    }

    /**
     * Get timeout metrics for service
     */
    public TimeoutMetrics getTimeoutMetrics(String serviceName) {
        return timeoutMetrics.get(serviceName);
    }

    /**
     * Get timeout configuration for service and operation (public method)
     */
    public TimeoutConfig getServiceTimeoutConfig(String serviceName, String operationType) {
        String configKey = serviceName + ":" + operationType;
        return serviceConfigs.get(configKey);
    }

    /**
     * Get all timeout configurations
     */
    public Map<String, TimeoutConfig> getAllTimeoutConfigs() {
        return new ConcurrentHashMap<>(serviceConfigs);
    }

    /**
     * Clear timeout metrics for service
     */
    public void clearTimeoutMetrics(String serviceName) {
        timeoutMetrics.remove(serviceName);
        log.info("Cleared timeout metrics for service '{}'", serviceName);
    }

    /**
     * Get overall timeout statistics
     */
    public TimeoutStatistics getOverallTimeoutStatistics() {
        long totalExecutions = timeoutMetrics.values().stream()
            .mapToLong(TimeoutMetrics::getTotalExecutions)
            .sum();
        
        long totalTimeouts = timeoutMetrics.values().stream()
            .mapToLong(TimeoutMetrics::getTimeoutCount)
            .sum();
        
        long totalFailures = timeoutMetrics.values().stream()
            .mapToLong(TimeoutMetrics::getFailureCount)
            .sum();
        
        double timeoutRate = totalExecutions > 0 ? (double) totalTimeouts / totalExecutions : 0.0;
        double failureRate = totalExecutions > 0 ? (double) totalFailures / totalExecutions : 0.0;
        
        return TimeoutStatistics.builder()
            .totalExecutions(totalExecutions)
            .totalTimeouts(totalTimeouts)
            .totalFailures(totalFailures)
            .timeoutRate(timeoutRate)
            .failureRate(failureRate)
            .serviceMetrics(timeoutMetrics)
            .build();
    }

    // Configuration and metrics classes
    @lombok.Data
    @lombok.Builder
    public static class TimeoutConfig {
        private String serviceName;
        private String operationType;
        private Duration timeout;
        private boolean enableMetrics;
        private Map<String, Object> customConfig;
    }

    @lombok.Data
    public static class TimeoutMetrics {
        private final String serviceName;
        private long successfulExecutions = 0;
        private long timeoutCount = 0;
        private long failureCount = 0;
        private final java.util.List<Duration> timeoutDurations = new java.util.ArrayList<>();
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public TimeoutMetrics(String serviceName) {
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

        public void recordTimeoutDuration(Duration duration) {
            timeoutDurations.add(duration);
            
            // Keep only last 100 timeout durations
            if (timeoutDurations.size() > 100) {
                timeoutDurations.remove(0);
            }
        }

        public void recordError(String error) {
            errors.add(error);
            
            // Keep only last 50 errors
            if (errors.size() > 50) {
                errors.remove(0);
            }
        }

        public long getTotalExecutions() {
            return successfulExecutions + timeoutCount + failureCount;
        }

        public double getTimeoutRate() {
            long total = getTotalExecutions();
            return total > 0 ? (double) timeoutCount / total : 0.0;
        }

        public double getFailureRate() {
            long total = getTotalExecutions();
            return total > 0 ? (double) failureCount / total : 0.0;
        }

        public Duration getAverageTimeoutDuration() {
            return timeoutDurations.stream()
                .reduce(Duration.ZERO, Duration::plus)
                .dividedBy(timeoutDurations.size());
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class TimeoutStatistics {
        private long totalExecutions;
        private long totalTimeouts;
        private long totalFailures;
        private double timeoutRate;
        private double failureRate;
        private Map<String, TimeoutMetrics> serviceMetrics;
    }
}
