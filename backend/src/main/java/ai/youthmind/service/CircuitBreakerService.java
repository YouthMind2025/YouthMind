package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CircuitBreakerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerService.class);
    
    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    public <T> T executeWithCircuitBreaker(String serviceName, SupplierWithException<T> operation, 
                                          SupplierWithException<T> fallback) {
        CircuitBreaker breaker = getOrCreateCircuitBreaker(serviceName);
        
        if (breaker.getState() == CircuitBreakerState.OPEN) {
            logger.warn("Circuit breaker is OPEN for service: {}", serviceName);
            try {
                return fallback.get();
            } catch (Exception e) {
                logger.error("Fallback failed for service {}: {}", serviceName, e.getMessage());
                throw new RuntimeException("Both primary and fallback operations failed", e);
            }
        }
        
        try {
            T result = operation.get();
            breaker.recordSuccess();
            return result;
        } catch (Exception e) {
            breaker.recordFailure();
            logger.error("Operation failed for service {}: {}", serviceName, e.getMessage());
            
            if (breaker.getState() == CircuitBreakerState.OPEN) {
                logger.warn("Circuit breaker opened for service: {}", serviceName);
            }
            
            try {
                return fallback.get();
            } catch (Exception fallbackException) {
                logger.error("Fallback failed for service {}: {}", serviceName, fallbackException.getMessage());
                throw new RuntimeException("Both primary and fallback operations failed", fallbackException);
            }
        }
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
        return circuitBreakers.computeIfAbsent(serviceName, k -> new CircuitBreaker(serviceName));
    }
    
    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }
    
    private static class CircuitBreaker {
        private final String serviceName;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private volatile LocalDateTime lastFailureTime;
        private volatile LocalDateTime lastStateChange = LocalDateTime.now();
        
        private static final int FAILURE_THRESHOLD = 5;
        private static final int SUCCESS_THRESHOLD = 3;
        private static final long TIMEOUT_DURATION_SECONDS = 60;
        
        public CircuitBreaker(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public synchronized void recordSuccess() {
            successCount.incrementAndGet();
            
            if (state == CircuitBreakerState.HALF_OPEN && successCount.get() >= SUCCESS_THRESHOLD) {
                state = CircuitBreakerState.CLOSED;
                failureCount.set(0);
                successCount.set(0);
                lastStateChange = LocalDateTime.now();
                logger.info("Circuit breaker CLOSED for service: {}", serviceName);
            }
        }
        
        public synchronized void recordFailure() {
            failureCount.incrementAndGet();
            lastFailureTime = LocalDateTime.now();
            
            if (state == CircuitBreakerState.CLOSED && failureCount.get() >= FAILURE_THRESHOLD) {
                state = CircuitBreakerState.OPEN;
                lastStateChange = LocalDateTime.now();
                logger.warn("Circuit breaker OPENED for service: {}", serviceName);
            } else if (state == CircuitBreakerState.HALF_OPEN) {
                state = CircuitBreakerState.OPEN;
                successCount.set(0);
                lastStateChange = LocalDateTime.now();
                logger.warn("Circuit breaker returned to OPEN for service: {}", serviceName);
            }
        }
        
        public CircuitBreakerState getState() {
            if (state == CircuitBreakerState.OPEN) {
                if (LocalDateTime.now().isAfter(lastStateChange.plusSeconds(TIMEOUT_DURATION_SECONDS))) {
                    state = CircuitBreakerState.HALF_OPEN;
                    successCount.set(0);
                    lastStateChange = LocalDateTime.now();
                    logger.info("Circuit breaker moved to HALF_OPEN for service: {}", serviceName);
                }
            }
            return state;
        }
    }
    
    private enum CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
}
