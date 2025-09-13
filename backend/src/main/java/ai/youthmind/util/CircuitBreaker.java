package ai.youthmind.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation for AI service calls
 * Provides fault tolerance and prevents cascading failures
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
    
    public CircuitBreaker() {
        this(5, 60000, 3); // 5 failures, 60s timeout, 3 half-open calls
    }
    
    public CircuitBreaker(int failureThreshold, long timeoutDurationMs, long halfOpenMaxCalls) {
        this.failureThreshold = failureThreshold;
        this.timeoutDurationMs = timeoutDurationMs;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
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
}
