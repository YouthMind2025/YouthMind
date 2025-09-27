package ai.youthmind.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced Circuit Breaker implementation for AI service calls with advanced resilience patterns.
 * Features:
 * - Configurable failure thresholds and timeouts
 * - State transitions (CLOSED, OPEN, HALF_OPEN)
 * - Automatic recovery and health monitoring
 * - Request timeout handling and cancellation
 * - Comprehensive metrics and monitoring
 * - Multiple service support with individual configurations
 */
@Component
public class CircuitBreaker {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    // Circuit breaker states
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, calls are blocked
        HALF_OPEN  // Testing if service is back
    }
    
    // Configuration
    private final int failureThreshold;
    private final long timeoutDurationMs;
    private final long halfOpenMaxCalls;
    
    // State tracking
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicReference<LocalDateTime> lastFailureTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastSuccessTime = new AtomicReference<>();
    
    // Half-open state tracking
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
    
    // Enhanced features for multiple services
    private final Map<String, CircuitBreakerConfig> serviceConfigs = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreakerState> serviceStates = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreakerMetrics> serviceMetrics = new ConcurrentHashMap<>();
    
    public CircuitBreaker() {
        this(5, 60000, 3); // 5 failures, 60s timeout, 3 half-open calls
    }
    
    public CircuitBreaker(int failureThreshold, long timeoutDurationMs, long halfOpenMaxCalls) {
        this.failureThreshold = failureThreshold;
        this.timeoutDurationMs = timeoutDurationMs;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
    }
    
    /**
     * Enhanced execute method with service-specific configuration
     */
    public <T> CompletableFuture<T> executeWithCircuitBreaker(
            String serviceName,
            CircuitBreakerCallable<T> operation,
            CircuitBreakerCallable<T> fallback) {
        
        return executeWithCircuitBreaker(serviceName, operation, fallback, null);
    }
    
    /**
     * Enhanced execute method with custom timeout
     */
    public <T> CompletableFuture<T> executeWithCircuitBreaker(
            String serviceName,
            CircuitBreakerCallable<T> operation,
            CircuitBreakerCallable<T> fallback,
            Duration customTimeout) {
        
        return CompletableFuture.supplyAsync(() -> {
            CircuitBreakerState state = getOrCreateServiceState(serviceName);
            CircuitBreakerConfig config = getServiceConfig(serviceName);
            
            try {
                // Check circuit state
                if (state.getCurrentState() == State.OPEN) {
                    if (state.canAttemptReset()) {
                        logger.info("Circuit breaker for service '{}' attempting reset (HALF_OPEN)", serviceName);
                        state.transitionToHalfOpen();
                    } else {
                        logger.warn("Circuit breaker for service '{}' is OPEN, executing fallback", serviceName);
                        recordFallbackExecution(serviceName);
                        try {
                    return fallback.call();
                } catch (Exception fallbackException) {
                    logger.error("Fallback also failed for service '{}': {}", serviceName, fallbackException.getMessage());
                    throw new RuntimeException("Both operation and fallback failed", fallbackException);
                }
                    }
                }
                
                // Execute operation with timeout
                Duration timeout = customTimeout != null ? customTimeout : Duration.ofMillis(config.getTimeoutMs());
                CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return operation.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                
                T result = future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                
                // Success - handle based on current state
                handleSuccess(state, serviceName);
                recordSuccess(serviceName);
                
                return result;
                
            } catch (TimeoutException e) {
                logger.warn("Operation timeout for service '{}': {}", serviceName, e.getMessage());
                handleFailure(state, serviceName, "Timeout");
                recordFailure(serviceName, "Timeout");
                try {
                    return fallback.call();
                } catch (Exception fallbackException) {
                    logger.error("Fallback also failed for service '{}': {}", serviceName, fallbackException.getMessage());
                    throw new RuntimeException("Both operation and fallback failed", fallbackException);
                }
                
            } catch (Exception e) {
                logger.error("Operation failed for service '{}': {}", serviceName, e.getMessage(), e);
                handleFailure(state, serviceName, e.getMessage());
                recordFailure(serviceName, e.getMessage());
                try {
                    return fallback.call();
                } catch (Exception fallbackException) {
                    logger.error("Fallback also failed for service '{}': {}", serviceName, fallbackException.getMessage());
                    throw new RuntimeException("Both operation and fallback failed", fallbackException);
                }
            }
        });
    }
    
    /**
     * Execute a call with circuit breaker protection
     */
    public <T> T execute(CircuitBreakerCallable<T> callable) throws Exception {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return executeInClosedState(callable);
            case OPEN:
                return handleOpenState(callable);
            case HALF_OPEN:
                return executeInHalfOpenState(callable);
            default:
                throw new IllegalStateException("Unknown circuit breaker state: " + currentState);
        }
    }
    
    /**
     * Execute call in CLOSED state (normal operation)
     */
    private <T> T executeInClosedState(CircuitBreakerCallable<T> callable) throws Exception {
        try {
            T result = callable.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    /**
     * Handle call in OPEN state
     */
    private <T> T handleOpenState(CircuitBreakerCallable<T> callable) throws Exception {
        if (shouldAttemptReset()) {
            logger.info("Circuit breaker attempting to transition to HALF_OPEN state");
            state.set(State.HALF_OPEN);
            halfOpenCalls.set(0);
            return executeInHalfOpenState(callable);
        } else {
            logger.warn("Circuit breaker is OPEN - call blocked");
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN - service unavailable");
        }
    }
    
    /**
     * Execute call in HALF_OPEN state (testing if service is back)
     */
    private <T> T executeInHalfOpenState(CircuitBreakerCallable<T> callable) throws Exception {
        int currentCalls = halfOpenCalls.incrementAndGet();
        
        if (currentCalls > halfOpenMaxCalls) {
            logger.warn("Half-open call limit exceeded - transitioning back to OPEN");
            state.set(State.OPEN);
            throw new CircuitBreakerOpenException("Circuit breaker half-open call limit exceeded");
        }
        
        try {
            T result = callable.call();
            onSuccess();
            logger.info("Circuit breaker transitioning to CLOSED state after successful call");
            state.set(State.CLOSED);
            failureCount.set(0);
            halfOpenCalls.set(0);
            return result;
        } catch (Exception e) {
            onFailure();
            logger.warn("Circuit breaker transitioning back to OPEN state after failed call");
            state.set(State.OPEN);
            halfOpenCalls.set(0);
            throw e;
        }
    }
    
    /**
     * Handle successful call
     */
    private void onSuccess() {
        successCount.incrementAndGet();
        lastSuccessTime.set(LocalDateTime.now());
        logger.debug("Circuit breaker recorded success - count: {}", successCount.get());
    }
    
    /**
     * Handle failed call
     */
    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(LocalDateTime.now());
        logger.warn("Circuit breaker recorded failure - count: {}", failures);
        
        if (failures >= failureThreshold) {
            logger.error("Circuit breaker threshold exceeded - transitioning to OPEN state");
            state.set(State.OPEN);
        }
    }
    
    /**
     * Check if circuit breaker should attempt reset
     */
    private boolean shouldAttemptReset() {
        LocalDateTime lastFailure = lastFailureTime.get();
        if (lastFailure == null) {
            return true;
        }
        
        long timeSinceLastFailure = ChronoUnit.MILLIS.between(lastFailure, LocalDateTime.now());
        return timeSinceLastFailure >= timeoutDurationMs;
    }
    
    /**
     * Get current circuit breaker state
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Get failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Get success count
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * Get last failure time
     */
    public LocalDateTime getLastFailureTime() {
        return lastFailureTime.get();
    }
    
    /**
     * Get last success time
     */
    public LocalDateTime getLastSuccessTime() {
        return lastSuccessTime.get();
    }
    
    /**
     * Reset circuit breaker to CLOSED state
     */
    public void reset() {
        logger.info("Circuit breaker manually reset to CLOSED state");
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        halfOpenCalls.set(0);
        lastFailureTime.set(null);
        lastSuccessTime.set(null);
    }
    
    /**
     * Get circuit breaker health status
     */
    public CircuitBreakerHealth getHealth() {
        return new CircuitBreakerHealth(
            state.get(),
            failureCount.get(),
            successCount.get(),
            lastFailureTime.get(),
            lastSuccessTime.get(),
            halfOpenCalls.get()
        );
    }
    
    /**
     * Functional interface for circuit breaker calls
     */
    @FunctionalInterface
    public interface CircuitBreakerCallable<T> {
        T call() throws Exception;
    }
    
    /**
     * Exception thrown when circuit breaker is open
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
    
    /**
     * Circuit breaker health information
     */
    public static class CircuitBreakerHealth {
        private final State state;
        private final int failureCount;
        private final int successCount;
        private final LocalDateTime lastFailureTime;
        private final LocalDateTime lastSuccessTime;
        private final int halfOpenCalls;
        
        public CircuitBreakerHealth(State state, int failureCount, int successCount, 
                                  LocalDateTime lastFailureTime, LocalDateTime lastSuccessTime, 
                                  int halfOpenCalls) {
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.lastFailureTime = lastFailureTime;
            this.lastSuccessTime = lastSuccessTime;
            this.halfOpenCalls = halfOpenCalls;
        }
        
        // Getters
        public State getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public LocalDateTime getLastFailureTime() { return lastFailureTime; }
        public LocalDateTime getLastSuccessTime() { return lastSuccessTime; }
        public int getHalfOpenCalls() { return halfOpenCalls; }
    }
    
    // Enhanced helper methods
    private CircuitBreakerState getOrCreateServiceState(String serviceName) {
        return serviceStates.computeIfAbsent(serviceName, name -> {
            CircuitBreakerConfig config = getServiceConfig(name);
            return new CircuitBreakerState(name, config);
        });
    }
    
    private CircuitBreakerConfig getServiceConfig(String serviceName) {
        return serviceConfigs.computeIfAbsent(serviceName, name -> {
            return createDefaultConfig(name);
        });
    }
    
    private CircuitBreakerConfig createDefaultConfig(String serviceName) {
        return CircuitBreakerConfig.builder()
            .serviceName(serviceName)
            .failureThreshold(5)
            .successThreshold(2)
            .timeoutMs(30000)
            .resetTimeoutMs(60000)
            .enableMetrics(true)
            .build();
    }
    
    private void handleSuccess(CircuitBreakerState state, String serviceName) {
        if (state.getCurrentState() == State.HALF_OPEN) {
            state.incrementSuccessCount();
            CircuitBreakerConfig config = getServiceConfig(serviceName);
            
            if (state.getSuccessCount().get() >= config.getSuccessThreshold()) {
                state.transitionToClosed();
                logger.info("Circuit breaker for service '{}' transitioned to CLOSED", serviceName);
            }
        } else if (state.getCurrentState() == State.CLOSED) {
            state.resetFailureCount();
        }
    }
    
    private void handleFailure(CircuitBreakerState state, String serviceName, String errorMessage) {
        if (state.getCurrentState() == State.HALF_OPEN) {
            state.transitionToOpen();
            logger.warn("Circuit breaker for service '{}' transitioned to OPEN due to failure in HALF_OPEN state", serviceName);
        } else if (state.getCurrentState() == State.CLOSED) {
            state.incrementFailureCount();
            CircuitBreakerConfig config = getServiceConfig(serviceName);
            
            if (state.getFailureCount().get() >= config.getFailureThreshold()) {
                state.transitionToOpen();
                logger.warn("Circuit breaker for service '{}' transitioned to OPEN due to {} consecutive failures", 
                        serviceName, state.getFailureCount().get());
            }
        }
    }
    
    private void recordSuccess(String serviceName) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementSuccessCount();
    }
    
    private void recordFailure(String serviceName, String errorMessage) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementFailureCount();
        metrics.recordError(errorMessage);
    }
    
    private void recordFallbackExecution(String serviceName) {
        CircuitBreakerMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.incrementFallbackCount();
    }
    
    private CircuitBreakerMetrics getOrCreateMetrics(String serviceName) {
        return serviceMetrics.computeIfAbsent(serviceName, name -> new CircuitBreakerMetrics(name));
    }
    
    /**
     * Get circuit breaker health status for all services
     */
    public CircuitBreakerHealthStatus getHealthStatus() {
        Map<String, ServiceHealth> serviceHealths = new ConcurrentHashMap<>();
        
        for (String serviceName : serviceStates.keySet()) {
            CircuitBreakerState state = serviceStates.get(serviceName);
            CircuitBreakerMetrics metrics = serviceMetrics.get(serviceName);
            
            ServiceHealth health = ServiceHealth.builder()
                .serviceName(serviceName)
                .currentState(state.getCurrentState())
                .failureCount(state.getFailureCount().get())
                .successCount(state.getSuccessCount().get())
                .lastFailureTime(state.getLastFailureTime())
                .totalRequests(metrics != null ? metrics.getTotalRequests().get() : 0)
                .successRate(metrics != null ? metrics.getSuccessRate() : 0.0)
                .fallbackRate(metrics != null ? metrics.getFallbackRate() : 0.0)
                .build();
            
            serviceHealths.put(serviceName, health);
        }
        
        return CircuitBreakerHealthStatus.builder()
            .overallHealth(calculateOverallHealth())
            .serviceHealths(serviceHealths)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    private double calculateOverallHealth() {
        if (serviceStates.isEmpty()) {
            return 1.0;
        }
        
        long openCircuits = serviceStates.values().stream()
            .mapToLong(state -> state.getCurrentState() == State.OPEN ? 1 : 0)
            .sum();
        
        long halfOpenCircuits = serviceStates.values().stream()
            .mapToLong(state -> state.getCurrentState() == State.HALF_OPEN ? 1 : 0)
            .sum();
        
        long totalCircuits = serviceStates.size();
        
        return (double) (totalCircuits - openCircuits - (halfOpenCircuits * 0.5)) / totalCircuits;
    }
    
    // Enhanced configuration and state classes
    @lombok.Data
    @lombok.Builder
    public static class CircuitBreakerConfig {
        private String serviceName;
        private int failureThreshold;
        private int successThreshold;
        private long timeoutMs;
        private long resetTimeoutMs;
        private boolean enableMetrics;
    }
    
    @lombok.Data
    public static class CircuitBreakerState {
        private final String serviceName;
        private volatile State currentState;
        private final AtomicInteger failureCount;
        private final AtomicInteger successCount;
        private volatile LocalDateTime lastFailureTime;
        private final CircuitBreakerConfig config;

        public CircuitBreakerState(String serviceName, CircuitBreakerConfig config) {
            this.serviceName = serviceName;
            this.currentState = State.CLOSED;
            this.failureCount = new AtomicInteger(0);
            this.successCount = new AtomicInteger(0);
            this.config = config;
        }

        public void transitionToOpen() {
            currentState = State.OPEN;
            lastFailureTime = LocalDateTime.now();
            successCount.set(0);
        }

        public void transitionToHalfOpen() {
            currentState = State.HALF_OPEN;
            failureCount.set(0);
        }

        public void transitionToClosed() {
            currentState = State.CLOSED;
            failureCount.set(0);
            successCount.set(0);
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }

        public void resetFailureCount() {
            failureCount.set(0);
        }

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        public boolean canAttemptReset() {
            return currentState == State.OPEN &&
                   lastFailureTime != null &&
                   Duration.between(lastFailureTime, LocalDateTime.now()).compareTo(Duration.ofMillis(config.getResetTimeoutMs())) > 0;
        }
    }
    
    @lombok.Data
    public static class CircuitBreakerMetrics {
        private final String serviceName;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong fallbackCount = new AtomicLong(0);
        private final AtomicLong totalRequests = new AtomicLong(0);

        public CircuitBreakerMetrics(String serviceName) {
            this.serviceName = serviceName;
        }

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
            totalRequests.incrementAndGet();
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
            totalRequests.incrementAndGet();
        }

        public void incrementFallbackCount() {
            fallbackCount.incrementAndGet();
        }

        public void recordError(String errorMessage) {
            // Could be extended to track specific error types
        }

        public double getSuccessRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) successCount.get() / total : 0.0;
        }

        public double getFallbackRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) fallbackCount.get() / total : 0.0;
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class CircuitBreakerHealthStatus {
        private double overallHealth;
        private Map<String, ServiceHealth> serviceHealths;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class ServiceHealth {
        private String serviceName;
        private State currentState;
        private int failureCount;
        private int successCount;
        private LocalDateTime lastFailureTime;
        private long totalRequests;
        private double successRate;
        private double fallbackRate;
    }
}
