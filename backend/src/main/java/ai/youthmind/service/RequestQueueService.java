package ai.youthmind.service;

import ai.youthmind.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis-based request queuing and load balancing service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrencyManager concurrencyManager;

    // Queue names
    private static final String HIGH_PRIORITY_QUEUE = "queue:high_priority";
    private static final String NORMAL_PRIORITY_QUEUE = "queue:normal_priority";
    private static final String LOW_PRIORITY_QUEUE = "queue:low_priority";
    private static final String BATCH_QUEUE = "queue:batch";
    private static final String AI_QUEUE = "queue:ai";
    private static final String VECTOR_SEARCH_QUEUE = "queue:vector_search";

    // Metrics
    private final AtomicLong totalQueuedRequests = new AtomicLong(0);
    private final AtomicLong totalProcessedRequests = new AtomicLong(0);
    private final AtomicLong totalFailedRequests = new AtomicLong(0);
    
    // Processing time tracking
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong processingTimeCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> processingTimes = new ConcurrentHashMap<>();

    /**
     * Queue a request with priority
     */
    public CompletableFuture<String> queueRequest(RequestQueueItem request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PerformanceMonitor.start("request_queuing");
                
                // Generate request ID
                String requestId = generateRequestId();
                request.setRequestId(requestId);
                request.setQueuedAt(LocalDateTime.now());
                request.setStatus(RequestStatus.QUEUED);
                
                // Determine queue based on priority and type
                String queueName = determineQueue(request);
                
                // Add to appropriate queue with priority score
                double priorityScore = calculatePriorityScore(request);
                redisTemplate.opsForZSet().add(queueName, request, priorityScore);
                
                // Set TTL for request (1 hour)
                redisTemplate.expire(requestId, 3600, TimeUnit.SECONDS);
                
                // Update metrics
                totalQueuedRequests.incrementAndGet();
                
                PerformanceMonitor.end("request_queuing");
                log.debug("Request {} queued in {} with priority {}", 
                    requestId, queueName, priorityScore);
                
                return requestId;
                
            } catch (Exception e) {
                log.error("Error queuing request: {}", e.getMessage(), e);
                throw new RuntimeException("Request queuing failed", e);
            }
        });
    }

    /**
     * Process requests from queue
     */
    public CompletableFuture<RequestQueueItem> processNextRequest(String queueName) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                PerformanceMonitor.start("request_processing");
                
                // Get highest priority request from queue
                Set<Object> requests = redisTemplate.opsForZSet().range(queueName, 0, 0);
                
                if (requests.isEmpty()) {
                    return null; // No requests in queue
                }
                
                RequestQueueItem request = (RequestQueueItem) requests.iterator().next();
                
                // Check if system can handle the request
                if (!concurrencyManager.canHandleRequest(request.getUserId())) {
                    // Move to back of queue with lower priority
                    redisTemplate.opsForZSet().remove(queueName, request);
                    redisTemplate.opsForZSet().add(queueName, request, 
                        calculatePriorityScore(request) - 1);
                    return null;
                }
                
                // Remove from queue
                redisTemplate.opsForZSet().remove(queueName, request);
                
                // Update request status
                request.setStatus(RequestStatus.PROCESSING);
                request.setProcessingStartedAt(LocalDateTime.now());
                
                // Update metrics
                totalProcessedRequests.incrementAndGet();
                
                // Record processing start time for this request
                processingTimes.put(request.getRequestId(), startTime);
                
                PerformanceMonitor.end("request_processing");
                log.debug("Processing request {} from queue {}", request.getRequestId(), queueName);
                
                return request;
                
            } catch (Exception e) {
                log.error("Error processing request from queue {}: {}", queueName, e.getMessage(), e);
                totalFailedRequests.incrementAndGet();
                return null;
            }
        });
    }

    /**
     * Complete request processing
     */
    public void completeRequest(String requestId, boolean success, Object result) {
        try {
            RequestQueueItem request = getRequest(requestId);
            if (request != null) {
                request.setStatus(success ? RequestStatus.COMPLETED : RequestStatus.FAILED);
                request.setCompletedAt(LocalDateTime.now());
                request.setResult(result);
                
                // Calculate and record processing time
                Long startTime = processingTimes.remove(requestId);
                if (startTime != null) {
                    long processingTime = System.currentTimeMillis() - startTime;
                    recordProcessingTime(processingTime);
                    request.setProcessingTimeMs(processingTime);
                }
                
                // Update metrics
                if (!success) {
                    totalFailedRequests.incrementAndGet();
                }
                
                // Cache result for 1 hour
                redisTemplate.opsForValue().set("request:result:" + requestId, request, 3600, TimeUnit.SECONDS);
                
                log.debug("Request {} completed with status {} in {}ms", 
                    requestId, request.getStatus(), 
                    request.getProcessingTimeMs() != null ? request.getProcessingTimeMs() : 0);
            }
        } catch (Exception e) {
            log.error("Error completing request {}: {}", requestId, e.getMessage(), e);
        }
    }

    /**
     * Get request status
     */
    public RequestQueueItem getRequest(String requestId) {
        try {
            return (RequestQueueItem) redisTemplate.opsForValue().get("request:result:" + requestId);
        } catch (Exception e) {
            log.error("Error getting request {}: {}", requestId, e.getMessage());
            return null;
        }
    }

    /**
     * Get queue statistics
     */
    public QueueStatistics getQueueStatistics() {
        try {
            Map<String, Long> queueSizes = new HashMap<>();
            queueSizes.put("high_priority", redisTemplate.opsForZSet().size(HIGH_PRIORITY_QUEUE));
            queueSizes.put("normal_priority", redisTemplate.opsForZSet().size(NORMAL_PRIORITY_QUEUE));
            queueSizes.put("low_priority", redisTemplate.opsForZSet().size(LOW_PRIORITY_QUEUE));
            queueSizes.put("batch", redisTemplate.opsForZSet().size(BATCH_QUEUE));
            queueSizes.put("ai", redisTemplate.opsForZSet().size(AI_QUEUE));
            queueSizes.put("vector_search", redisTemplate.opsForZSet().size(VECTOR_SEARCH_QUEUE));
            
            return QueueStatistics.builder()
                .queueSizes(queueSizes)
                .totalQueuedRequests(totalQueuedRequests.get())
                .totalProcessedRequests(totalProcessedRequests.get())
                .totalFailedRequests(totalFailedRequests.get())
                .averageProcessingTime(calculateAverageProcessingTime())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting queue statistics: {}", e.getMessage());
            return QueueStatistics.builder()
                .queueSizes(new HashMap<>())
                .totalQueuedRequests(0)
                .totalProcessedRequests(0)
                .totalFailedRequests(0)
                .averageProcessingTime(0.0)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Load balance requests across multiple instances
     */
    public CompletableFuture<String> loadBalanceRequest(RequestQueueItem request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get available instances
                List<String> availableInstances = getAvailableInstances();
                
                if (availableInstances.isEmpty()) {
                    // No instances available, queue the request
                    return queueRequest(request).join();
                }
                
                // Select instance using round-robin or least-loaded strategy
                String selectedInstance = selectInstance(availableInstances, request);
                
                // Route request to selected instance
                return routeRequestToInstance(request, selectedInstance);
                
            } catch (Exception e) {
                log.error("Error in load balancing: {}", e.getMessage(), e);
                // Fallback to queuing
                return queueRequest(request).join();
            }
        });
    }

    // Private helper methods
    private String determineQueue(RequestQueueItem request) {
        switch (request.getPriority()) {
            case HIGH:
                return HIGH_PRIORITY_QUEUE;
            case LOW:
                return LOW_PRIORITY_QUEUE;
            case BATCH:
                return BATCH_QUEUE;
            case AI:
                return AI_QUEUE;
            case VECTOR_SEARCH:
                return VECTOR_SEARCH_QUEUE;
            default:
                return NORMAL_PRIORITY_QUEUE;
        }
    }

    private double calculatePriorityScore(RequestQueueItem request) {
        double baseScore = request.getPriority().getScore();
        
        // Adjust based on user tier
        if (request.getUserTier() != null) {
            baseScore += request.getUserTier().getPriorityBoost();
        }
        
        // Adjust based on request age (older requests get higher priority)
        long ageMinutes = java.time.Duration.between(request.getQueuedAt(), LocalDateTime.now()).toMinutes();
        baseScore += ageMinutes * 0.1;
        
        // Adjust based on retry count (retried requests get lower priority)
        baseScore -= request.getRetryCount() * 0.5;
        
        return Math.max(0, baseScore);
    }

    private String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Record processing time for a request
     */
    private void recordProcessingTime(long processingTimeMs) {
        totalProcessingTime.addAndGet(processingTimeMs);
        processingTimeCount.incrementAndGet();
        
        // Log performance metrics
        if (processingTimeMs > 1000) { // Log slow requests
            log.warn("Slow request processing: {}ms", processingTimeMs);
        }
    }

    /**
     * Calculate actual average processing time
     */
    private double calculateAverageProcessingTime() {
        long count = processingTimeCount.get();
        if (count == 0) {
            return 0.0;
        }
        
        long total = totalProcessingTime.get();
        return (double) total / count;
    }
    
    /**
     * Get processing time statistics
     */
    public ProcessingTimeStats getProcessingTimeStats() {
        long count = processingTimeCount.get();
        if (count == 0) {
            return ProcessingTimeStats.builder()
                .averageTime(0.0)
                .totalRequests(0)
                .totalTime(0)
                .build();
        }
        
        long total = totalProcessingTime.get();
        double average = (double) total / count;
        
        return ProcessingTimeStats.builder()
            .averageTime(average)
            .totalRequests(count)
            .totalTime(total)
            .build();
    }

    private List<String> getAvailableInstances() {
        // Get available instances from Redis
        Set<String> instances = redisTemplate.keys("instance:*");
        List<String> availableInstances = new ArrayList<>();
        
        for (String instanceKey : instances) {
            try {
                Map<Object, Object> instanceInfo = redisTemplate.opsForHash().entries(instanceKey);
                if (instanceInfo.containsKey("status") && 
                    "available".equals(instanceInfo.get("status"))) {
                    availableInstances.add(instanceKey.replace("instance:", ""));
                }
            } catch (Exception e) {
                log.warn("Error checking instance {}: {}", instanceKey, e.getMessage());
            }
        }
        
        return availableInstances;
    }

    private String selectInstance(List<String> instances, RequestQueueItem request) {
        // Simple round-robin selection
        // In production, this could be more sophisticated (least loaded, geographic proximity, etc.)
        int index = (int) (System.currentTimeMillis() % instances.size());
        return instances.get(index);
    }

    private String routeRequestToInstance(RequestQueueItem request, String instanceId) {
        // Route request to specific instance
        String instanceQueue = "queue:instance:" + instanceId;
        redisTemplate.opsForZSet().add(instanceQueue, request, calculatePriorityScore(request));
        
        log.debug("Routed request {} to instance {}", request.getRequestId(), instanceId);
        return request.getRequestId();
    }

    // Enums and data classes
    public enum RequestPriority {
        HIGH(10.0),
        NORMAL(5.0),
        LOW(1.0),
        BATCH(3.0),
        AI(7.0),
        VECTOR_SEARCH(6.0);

        private final double score;

        RequestPriority(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

    public enum RequestStatus {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED,
        TIMEOUT
    }

    public enum UserTier {
        FREE(0.0),
        PREMIUM(2.0),
        ENTERPRISE(5.0);

        private final double priorityBoost;

        UserTier(double priorityBoost) {
            this.priorityBoost = priorityBoost;
        }

        public double getPriorityBoost() {
            return priorityBoost;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class RequestQueueItem {
        private String requestId;
        private String userId;
        private String operation;
        private Object data;
        private RequestPriority priority;
        private UserTier userTier;
        private RequestStatus status;
        private LocalDateTime queuedAt;
        private LocalDateTime processingStartedAt;
        private LocalDateTime completedAt;
        private Object result;
        private int retryCount;
        private String errorMessage;
        private Long processingTimeMs; // Processing time in milliseconds
    }

    @lombok.Data
    @lombok.Builder
    public static class QueueStatistics {
        private Map<String, Long> queueSizes;
        private long totalQueuedRequests;
        private long totalProcessedRequests;
        private long totalFailedRequests;
        private double averageProcessingTime;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProcessingTimeStats {
        private double averageTime;
        private long totalRequests;
        private long totalTime;
    }
}
