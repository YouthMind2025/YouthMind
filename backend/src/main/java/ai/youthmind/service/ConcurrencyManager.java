package ai.youthmind.service;

import ai.youthmind.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages concurrency and resource allocation for 10k concurrent users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrencyManager {

    private final RedisTemplate<String, Object> redisTemplate;

    // Concurrency metrics
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    // Rate limiting
    private final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();
    private final Map<String, Long> userLastRequest = new ConcurrentHashMap<>();

    // Circuit breaker for external services
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    // Resource pools
    private final Semaphore databaseConnections = new Semaphore(100);
    private final Semaphore aiServiceConnections = new Semaphore(20);
    private final Semaphore vectorSearchConnections = new Semaphore(50);

    /**
     * Check if system can handle new request
     */
    public boolean canHandleRequest(String userId) {
        // Check global limits
        if (activeRequests.get() >= 10000) {
            log.warn("System at capacity: {} active requests", activeRequests.get());
            return false;
        }

        // Check rate limiting per user
        if (!checkRateLimit(userId)) {
            log.warn("Rate limit exceeded for user: {}", userId);
            return false;
        }

        // Check resource availability
        if (!databaseConnections.tryAcquire()) {
            log.warn("Database connections exhausted");
            return false;
        }

        return true;
    }

    /**
     * Acquire resources for request processing
     */
    public RequestContext acquireResources(String userId, String operation) {
        try {
            PerformanceMonitor.start("resource_acquisition");
            
            // Increment counters
            activeRequests.incrementAndGet();
            totalRequests.incrementAndGet();
            
            // Update user last request time
            userLastRequest.put(userId, System.currentTimeMillis());
            
            // Acquire specific resources based on operation
            ResourcePool resources = acquireOperationResources(operation);
            
            PerformanceMonitor.end("resource_acquisition");
            
            return new RequestContext(userId, operation, resources, System.currentTimeMillis());
                
        } catch (Exception e) {
            log.error("Error acquiring resources for user {}: {}", userId, e.getMessage());
            releaseResources(new RequestContext(userId, operation, null, System.currentTimeMillis()));
            throw new RuntimeException("Resource acquisition failed", e);
        }
    }

    /**
     * Release resources after request completion
     */
    public void releaseResources(RequestContext context) {
        try {
            // Release specific resources
            if (context.getResources() != null) {
                releaseOperationResources(context.getResources());
            }
            
            // Decrement counters
            activeRequests.decrementAndGet();
            
            // Update performance metrics
            long processingTime = System.currentTimeMillis() - context.getStartTime();
            // PerformanceMonitor.recordMetric("request_processing_time", processingTime);
            
            log.debug("Resources released for user {} in {}ms", 
                context.getUserId(), processingTime);
                
        } catch (Exception e) {
            log.error("Error releasing resources for user {}: {}", 
                context.getUserId(), e.getMessage());
        }
    }

    /**
     * Handle request error
     */
    public void handleError(RequestContext context, Exception error) {
        totalErrors.incrementAndGet();
        
        // Update circuit breaker
        String serviceKey = getServiceKey(context.getOperation());
        CircuitBreaker circuitBreaker = circuitBreakers.computeIfAbsent(serviceKey, 
            k -> new CircuitBreaker(5, 60000, 30000)); // 5 failures, 60s timeout, 30s retry
        
        circuitBreaker.recordFailure();
        
        log.error("Request error for user {}: {}", context.getUserId(), error.getMessage());
    }

    /**
     * Get system health status
     */
    public SystemHealth getSystemHealth() {
        return SystemHealth.builder()
            .activeConnections(activeConnections.get())
            .activeRequests(activeRequests.get())
            .totalRequests(totalRequests.get())
            .totalErrors(totalErrors.get())
            .errorRate(calculateErrorRate())
            .databaseConnectionsAvailable(databaseConnections.availablePermits())
            .aiServiceConnectionsAvailable(aiServiceConnections.availablePermits())
            .vectorSearchConnectionsAvailable(vectorSearchConnections.availablePermits())
            .memoryUsage(getMemoryUsage())
            .cpuUsage(getCpuUsage())
            .build();
    }

    /**
     * Update resource limits based on scaling factor
     */
    public void updateResourceLimits(double scaleFactor) {
        try {
            // Update database connection limits
            int newDbLimit = (int) Math.max(10, Math.min(200, 100 * scaleFactor));
            int currentDbAvailable = databaseConnections.availablePermits();
            int dbDelta = newDbLimit - currentDbAvailable;
            
            if (dbDelta > 0) {
                // Increasing capacity: release additional permits
                databaseConnections.release(dbDelta);
                log.debug("Increased database connection capacity by {} (new limit: {})", dbDelta, newDbLimit);
            } else if (dbDelta < 0) {
                // Decreasing capacity: acquire excess permits (non-blocking with timeout)
                int excessPermits = -dbDelta;
                int acquired = 0;
                long timeoutMs = 100; // 100ms timeout to prevent blocking
                long endTime = System.currentTimeMillis() + timeoutMs;
                
                while (acquired < excessPermits && System.currentTimeMillis() < endTime) {
                    if (databaseConnections.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                        acquired++;
                    }
                }
                
                if (acquired < excessPermits) {
                    log.warn("Could not acquire all excess database permits. Acquired: {}/{}", 
                        acquired, excessPermits);
                } else {
                    log.debug("Reduced database connection capacity by {} (new limit: {})", 
                        excessPermits, newDbLimit);
                }
            }
            
            // Update AI service connection limits
            int newAiLimit = (int) Math.max(5, Math.min(50, 20 * scaleFactor));
            int currentAiAvailable = aiServiceConnections.availablePermits();
            int aiDelta = newAiLimit - currentAiAvailable;
            
            if (aiDelta > 0) {
                aiServiceConnections.release(aiDelta);
                log.debug("Increased AI service connection capacity by {} (new limit: {})", 
                    aiDelta, newAiLimit);
            } else if (aiDelta < 0) {
                int excessPermits = -aiDelta;
                int acquired = 0;
                long timeoutMs = 100;
                long endTime = System.currentTimeMillis() + timeoutMs;
                
                while (acquired < excessPermits && System.currentTimeMillis() < endTime) {
                    if (aiServiceConnections.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                        acquired++;
                    }
                }
                
                if (acquired < excessPermits) {
                    log.warn("Could not acquire all excess AI service permits. Acquired: {}/{}", 
                        acquired, excessPermits);
                } else {
                    log.debug("Reduced AI service connection capacity by {} (new limit: {})", 
                        excessPermits, newAiLimit);
                }
            }
            
            // Update vector search connection limits
            int newVectorLimit = (int) Math.max(10, Math.min(100, 50 * scaleFactor));
            int currentVectorAvailable = vectorSearchConnections.availablePermits();
            int vectorDelta = newVectorLimit - currentVectorAvailable;
            
            if (vectorDelta > 0) {
                vectorSearchConnections.release(vectorDelta);
                log.debug("Increased vector search connection capacity by {} (new limit: {})", 
                    vectorDelta, newVectorLimit);
            } else if (vectorDelta < 0) {
                int excessPermits = -vectorDelta;
                int acquired = 0;
                long timeoutMs = 100;
                long endTime = System.currentTimeMillis() + timeoutMs;
                
                while (acquired < excessPermits && System.currentTimeMillis() < endTime) {
                    if (vectorSearchConnections.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                        acquired++;
                    }
                }
                
                if (acquired < excessPermits) {
                    log.warn("Could not acquire all excess vector search permits. Acquired: {}/{}", 
                        acquired, excessPermits);
                } else {
                    log.debug("Reduced vector search connection capacity by {} (new limit: {})", 
                        excessPermits, newVectorLimit);
                }
            }
            
            log.info("Updated resource limits - DB: {} (available: {}), AI: {} (available: {}), Vector: {} (available: {})", 
                newDbLimit, databaseConnections.availablePermits(),
                newAiLimit, aiServiceConnections.availablePermits(),
                newVectorLimit, vectorSearchConnections.availablePermits());
                
        } catch (Exception e) {
            log.error("Error updating resource limits: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update resource limits", e);
        }
    }

    /**
     * Check if service is available (circuit breaker)
     */
    public boolean isServiceAvailable(String operation) {
        String serviceKey = getServiceKey(operation);
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceKey);
        return circuitBreaker == null || circuitBreaker.isAvailable();
    }

    // Private helper methods
    private boolean checkRateLimit(String userId) {
        RateLimiter rateLimiter = userRateLimiters.computeIfAbsent(userId, 
            k -> new RateLimiter(100, 60000)); // 100 requests per minute
        
        return rateLimiter.tryAcquire();
    }

    private ResourcePool acquireOperationResources(String operation) {
        ResourcePool resources = new ResourcePool();
        
        switch (operation.toLowerCase()) {
            case "ai_chat":
            case "ai_moderation":
                if (aiServiceConnections.tryAcquire()) {
                    resources.setAiServiceConnection(true);
                } else {
                    throw new RuntimeException("AI service connections unavailable");
                }
                break;
                
            case "vector_search":
                if (vectorSearchConnections.tryAcquire()) {
                    resources.setVectorSearchConnection(true);
                } else {
                    throw new RuntimeException("Vector search connections unavailable");
                }
                break;
        }
        
        return resources;
    }

    private void releaseOperationResources(ResourcePool resources) {
        if (resources.isDatabaseConnection()) {
            databaseConnections.release();
        }
        if (resources.isAiServiceConnection()) {
            aiServiceConnections.release();
        }
        if (resources.isVectorSearchConnection()) {
            vectorSearchConnections.release();
        }
    }

    private String getServiceKey(String operation) {
        if (operation.contains("ai")) return "ai_service";
        if (operation.contains("vector")) return "vector_search";
        if (operation.contains("database")) return "database";
        return "general";
    }

    private double calculateErrorRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (double) totalErrors.get() / total * 100.0;
    }

    private double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return (double) usedMemory / totalMemory * 100.0;
    }

    private double getCpuUsage() {
        // Simplified CPU usage calculation
        // In production, use a proper monitoring library
        return Math.random() * 100.0; // Placeholder
    }

    // Inner classes
    @lombok.Data
    @lombok.Builder
    public static class RequestContext {
        private String userId;
        private String operation;
        private ResourcePool resources;
        private long startTime;
    }

    @lombok.Data
    @lombok.Builder
    public static class ResourcePool {
        @lombok.Builder.Default
        private boolean databaseConnection = true; // Always acquired
        @lombok.Builder.Default
        private boolean aiServiceConnection = false;
        @lombok.Builder.Default
        private boolean vectorSearchConnection = false;
        
        public ResourcePool() {
            this.databaseConnection = true;
            this.aiServiceConnection = false;
            this.vectorSearchConnection = false;
        }
        
        public ResourcePool(boolean databaseConnection, boolean aiServiceConnection, boolean vectorSearchConnection) {
            this.databaseConnection = databaseConnection;
            this.aiServiceConnection = aiServiceConnection;
            this.vectorSearchConnection = vectorSearchConnection;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class SystemHealth {
        private int activeConnections;
        private int activeRequests;
        private long totalRequests;
        private long totalErrors;
        private double errorRate;
        private int databaseConnectionsAvailable;
        private int aiServiceConnectionsAvailable;
        private int vectorSearchConnectionsAvailable;
        private double memoryUsage;
        private double cpuUsage;
    }

    // Simple rate limiter implementation
    private static class RateLimiter {
        private final int maxRequests;
        private final long windowMs;
        private final AtomicInteger currentRequests = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        public RateLimiter(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
        }

        public boolean tryAcquire() {
            long now = System.currentTimeMillis();
            
            // Reset window if needed
            if (now - windowStart >= windowMs) {
                synchronized (this) {
                    if (now - windowStart >= windowMs) {
                        currentRequests.set(0);
                        windowStart = now;
                    }
                }
            }
            
            return currentRequests.incrementAndGet() <= maxRequests;
        }
    }

    // Simple circuit breaker implementation
    private static class CircuitBreaker {
        private final int failureThreshold;
        private final long timeoutMs;
        private final long retryMs;
        private final AtomicInteger failures = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;
        private volatile boolean isOpen = false;

        public CircuitBreaker(int failureThreshold, long timeoutMs, long retryMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutMs = timeoutMs;
            this.retryMs = retryMs;
        }

        public boolean isAvailable() {
            if (!isOpen) return true;
            
            long now = System.currentTimeMillis();
            if (now - lastFailureTime >= retryMs) {
                isOpen = false;
                failures.set(0);
                return true;
            }
            
            return false;
        }

        public void recordFailure() {
            int currentFailures = failures.incrementAndGet();
            lastFailureTime = System.currentTimeMillis();
            
            if (currentFailures >= failureThreshold) {
                isOpen = true;
            }
        }

        public void recordSuccess() {
            failures.set(0);
            isOpen = false;
        }
    }
}
