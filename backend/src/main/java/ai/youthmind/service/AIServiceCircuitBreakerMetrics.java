package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Circuit Breaker Metrics for monitoring and alerting.
 * Features:
 * - Real-time metrics collection for circuit breaker states
 * - Performance monitoring and analysis
 * - Alert generation for circuit breaker events
 * - Historical metrics tracking
 * - Integration with monitoring systems
 * - Custom metrics and dashboards
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceCircuitBreakerMetrics {

    // Metrics storage
    private final Map<String, CircuitBreakerServiceMetrics> serviceMetrics = new ConcurrentHashMap<>();
    
    // Global metrics
    private final GlobalCircuitBreakerMetrics globalMetrics = new GlobalCircuitBreakerMetrics();

    /**
     * Record circuit breaker state transition
     */
    public void recordStateTransition(String serviceName, String fromState, String toState) {
        CircuitBreakerServiceMetrics metrics = getOrCreateServiceMetrics(serviceName);
        metrics.recordStateTransition(fromState, toState);
        
        globalMetrics.incrementStateTransitions();
        
        log.info("Circuit breaker state transition for service '{}': {} -> {}", 
                serviceName, fromState, toState);
    }

    /**
     * Record successful operation
     */
    public void recordSuccessfulOperation(String serviceName, long executionTimeMs) {
        CircuitBreakerServiceMetrics metrics = getOrCreateServiceMetrics(serviceName);
        metrics.recordSuccessfulOperation(executionTimeMs);
        
        globalMetrics.incrementSuccessfulOperations();
        
        log.debug("Recorded successful operation for service '{}' in {}ms", serviceName, executionTimeMs);
    }

    /**
     * Record failed operation
     */
    public void recordFailedOperation(String serviceName, String errorType, long executionTimeMs) {
        CircuitBreakerServiceMetrics metrics = getOrCreateServiceMetrics(serviceName);
        metrics.recordFailedOperation(errorType, executionTimeMs);
        
        globalMetrics.incrementFailedOperations();
        
        log.debug("Recorded failed operation for service '{}' with error type '{}'", serviceName, errorType);
    }

    /**
     * Record fallback execution
     */
    public void recordFallbackExecution(String serviceName, long executionTimeMs) {
        CircuitBreakerServiceMetrics metrics = getOrCreateServiceMetrics(serviceName);
        metrics.recordFallbackExecution(executionTimeMs);
        
        globalMetrics.incrementFallbackExecutions();
        
        log.debug("Recorded fallback execution for service '{}' in {}ms", serviceName, executionTimeMs);
    }

    /**
     * Record timeout
     */
    public void recordTimeout(String serviceName, long timeoutMs) {
        CircuitBreakerServiceMetrics metrics = getOrCreateServiceMetrics(serviceName);
        metrics.recordTimeout(timeoutMs);
        
        globalMetrics.incrementTimeouts();
        
        log.debug("Recorded timeout for service '{}' after {}ms", serviceName, timeoutMs);
    }

    /**
     * Record retry attempt
     */
    public void recordRetryAttempt(String serviceName, int attemptNumber, long delayMs) {
        CircuitBreakerServiceMetrics metrics = getOrCreateServiceMetrics(serviceName);
        metrics.recordRetryAttempt(attemptNumber, delayMs);
        
        globalMetrics.incrementRetryAttempts();
        
        log.debug("Recorded retry attempt {} for service '{}' with delay {}ms", 
                attemptNumber, serviceName, delayMs);
    }

    /**
     * Get metrics for specific service
     */
    public CircuitBreakerServiceMetrics getServiceMetrics(String serviceName) {
        return serviceMetrics.get(serviceName);
    }

    /**
     * Get all service metrics
     */
    public Map<String, CircuitBreakerServiceMetrics> getAllServiceMetrics() {
        return new ConcurrentHashMap<>(serviceMetrics);
    }

    /**
     * Get global metrics
     */
    public GlobalCircuitBreakerMetrics getGlobalMetrics() {
        return globalMetrics;
    }

    /**
     * Get circuit breaker health summary
     */
    public CircuitBreakerHealthSummary getHealthSummary() {
        long totalServices = serviceMetrics.size();
        long healthyServices = serviceMetrics.values().stream()
            .mapToLong(metrics -> metrics.getCurrentState().equals("CLOSED") ? 1 : 0)
            .sum();
        
        long openServices = serviceMetrics.values().stream()
            .mapToLong(metrics -> metrics.getCurrentState().equals("OPEN") ? 1 : 0)
            .sum();
        
        long halfOpenServices = serviceMetrics.values().stream()
            .mapToLong(metrics -> metrics.getCurrentState().equals("HALF_OPEN") ? 1 : 0)
            .sum();
        
        double overallHealth = totalServices > 0 ? (double) healthyServices / totalServices : 1.0;
        
        return CircuitBreakerHealthSummary.builder()
            .overallHealth(overallHealth)
            .totalServices(totalServices)
            .healthyServices(healthyServices)
            .openServices(openServices)
            .halfOpenServices(halfOpenServices)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get performance summary
     */
    public CircuitBreakerPerformanceSummary getPerformanceSummary() {
        long totalOperations = globalMetrics.getTotalOperations().get();
        long successfulOperations = globalMetrics.getSuccessfulOperations().get();
        long failedOperations = globalMetrics.getFailedOperations().get();
        long fallbackExecutions = globalMetrics.getFallbackExecutions().get();
        long timeouts = globalMetrics.getTimeouts().get();
        
        double successRate = totalOperations > 0 ? (double) successfulOperations / totalOperations : 0.0;
        double failureRate = totalOperations > 0 ? (double) failedOperations / totalOperations : 0.0;
        double fallbackRate = totalOperations > 0 ? (double) fallbackExecutions / totalOperations : 0.0;
        double timeoutRate = totalOperations > 0 ? (double) timeouts / totalOperations : 0.0;
        
        return CircuitBreakerPerformanceSummary.builder()
            .totalOperations(totalOperations)
            .successfulOperations(successfulOperations)
            .failedOperations(failedOperations)
            .fallbackExecutions(fallbackExecutions)
            .timeouts(timeouts)
            .successRate(successRate)
            .failureRate(failureRate)
            .fallbackRate(fallbackRate)
            .timeoutRate(timeoutRate)
            .averageExecutionTime(calculateAverageExecutionTime())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Calculate average execution time across all services
     */
    private double calculateAverageExecutionTime() {
        return serviceMetrics.values().stream()
            .mapToDouble(CircuitBreakerServiceMetrics::getAverageExecutionTime)
            .average()
            .orElse(0.0);
    }

    /**
     * Get or create service metrics
     */
    private CircuitBreakerServiceMetrics getOrCreateServiceMetrics(String serviceName) {
        return serviceMetrics.computeIfAbsent(serviceName, name -> new CircuitBreakerServiceMetrics(name));
    }

    /**
     * Clear metrics for service
     */
    public void clearServiceMetrics(String serviceName) {
        serviceMetrics.remove(serviceName);
        log.info("Cleared metrics for service '{}'", serviceName);
    }

    /**
     * Clear all metrics
     */
    public void clearAllMetrics() {
        serviceMetrics.clear();
        globalMetrics.reset();
        log.info("Cleared all circuit breaker metrics");
    }

    /**
     * Get metrics export for external monitoring systems
     */
    public CircuitBreakerMetricsExport exportMetrics() {
        return CircuitBreakerMetricsExport.builder()
            .serviceMetrics(serviceMetrics)
            .globalMetrics(globalMetrics)
            .healthSummary(getHealthSummary())
            .performanceSummary(getPerformanceSummary())
            .timestamp(LocalDateTime.now())
            .build();
    }

    // Metrics classes
    @lombok.Data
    public static class CircuitBreakerServiceMetrics {
        private final String serviceName;
        private volatile String currentState = "CLOSED";
        private final AtomicLong successfulOperations = new AtomicLong(0);
        private final AtomicLong failedOperations = new AtomicLong(0);
        private final AtomicLong fallbackExecutions = new AtomicLong(0);
        private final AtomicLong timeouts = new AtomicLong(0);
        private final AtomicLong retryAttempts = new AtomicLong(0);
        private final AtomicLong stateTransitions = new AtomicLong(0);
        
        private final java.util.List<Long> executionTimes = new java.util.ArrayList<>();
        private final java.util.List<String> errorTypes = new java.util.ArrayList<>();
        private final java.util.List<StateTransition> stateTransitionHistory = new java.util.ArrayList<>();
        
        private LocalDateTime lastOperationTime;
        private LocalDateTime lastStateTransitionTime;

        public CircuitBreakerServiceMetrics(String serviceName) {
            this.serviceName = serviceName;
        }

        public void recordStateTransition(String fromState, String toState) {
            currentState = toState;
            stateTransitions.incrementAndGet();
            lastStateTransitionTime = LocalDateTime.now();
            
            StateTransition transition = StateTransition.builder()
                .fromState(fromState)
                .toState(toState)
                .timestamp(LocalDateTime.now())
                .build();
            
            stateTransitionHistory.add(transition);
            
            // Keep only last 100 state transitions
            if (stateTransitionHistory.size() > 100) {
                stateTransitionHistory.remove(0);
            }
        }

        public void recordSuccessfulOperation(long executionTimeMs) {
            successfulOperations.incrementAndGet();
            executionTimes.add(executionTimeMs);
            lastOperationTime = LocalDateTime.now();
            
            // Keep only last 1000 execution times
            if (executionTimes.size() > 1000) {
                executionTimes.remove(0);
            }
        }

        public void recordFailedOperation(String errorType, long executionTimeMs) {
            failedOperations.incrementAndGet();
            errorTypes.add(errorType);
            executionTimes.add(executionTimeMs);
            lastOperationTime = LocalDateTime.now();
            
            // Keep only last 100 error types
            if (errorTypes.size() > 100) {
                errorTypes.remove(0);
            }
        }

        public void recordFallbackExecution(long executionTimeMs) {
            fallbackExecutions.incrementAndGet();
            executionTimes.add(executionTimeMs);
            lastOperationTime = LocalDateTime.now();
        }

        public void recordTimeout(long timeoutMs) {
            timeouts.incrementAndGet();
            executionTimes.add(timeoutMs);
            lastOperationTime = LocalDateTime.now();
        }

        public void recordRetryAttempt(int attemptNumber, long delayMs) {
            retryAttempts.incrementAndGet();
        }

        public long getTotalOperations() {
            return successfulOperations.get() + failedOperations.get() + fallbackExecutions.get();
        }

        public double getSuccessRate() {
            long total = getTotalOperations();
            return total > 0 ? (double) successfulOperations.get() / total : 0.0;
        }

        public double getFailureRate() {
            long total = getTotalOperations();
            return total > 0 ? (double) failedOperations.get() / total : 0.0;
        }

        public double getFallbackRate() {
            long total = getTotalOperations();
            return total > 0 ? (double) fallbackExecutions.get() / total : 0.0;
        }

        public double getTimeoutRate() {
            long total = getTotalOperations();
            return total > 0 ? (double) timeouts.get() / total : 0.0;
        }

        public double getAverageExecutionTime() {
            return executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        }
    }

    @lombok.Data
    public static class GlobalCircuitBreakerMetrics {
        private final AtomicLong totalOperations = new AtomicLong(0);
        private final AtomicLong successfulOperations = new AtomicLong(0);
        private final AtomicLong failedOperations = new AtomicLong(0);
        private final AtomicLong fallbackExecutions = new AtomicLong(0);
        private final AtomicLong timeouts = new AtomicLong(0);
        private final AtomicLong retryAttempts = new AtomicLong(0);
        private final AtomicLong stateTransitions = new AtomicLong(0);

        public void incrementSuccessfulOperations() {
            successfulOperations.incrementAndGet();
            totalOperations.incrementAndGet();
        }

        public void incrementFailedOperations() {
            failedOperations.incrementAndGet();
            totalOperations.incrementAndGet();
        }

        public void incrementFallbackExecutions() {
            fallbackExecutions.incrementAndGet();
        }

        public void incrementTimeouts() {
            timeouts.incrementAndGet();
        }

        public void incrementRetryAttempts() {
            retryAttempts.incrementAndGet();
        }

        public void incrementStateTransitions() {
            stateTransitions.incrementAndGet();
        }

        public void reset() {
            totalOperations.set(0);
            successfulOperations.set(0);
            failedOperations.set(0);
            fallbackExecutions.set(0);
            timeouts.set(0);
            retryAttempts.set(0);
            stateTransitions.set(0);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class CircuitBreakerHealthSummary {
        private double overallHealth;
        private long totalServices;
        private long healthyServices;
        private long openServices;
        private long halfOpenServices;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class CircuitBreakerPerformanceSummary {
        private long totalOperations;
        private long successfulOperations;
        private long failedOperations;
        private long fallbackExecutions;
        private long timeouts;
        private double successRate;
        private double failureRate;
        private double fallbackRate;
        private double timeoutRate;
        private double averageExecutionTime;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class CircuitBreakerMetricsExport {
        private Map<String, CircuitBreakerServiceMetrics> serviceMetrics;
        private GlobalCircuitBreakerMetrics globalMetrics;
        private CircuitBreakerHealthSummary healthSummary;
        private CircuitBreakerPerformanceSummary performanceSummary;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class StateTransition {
        private String fromState;
        private String toState;
        private LocalDateTime timestamp;
    }
}
