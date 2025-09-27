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
    
    // Psychological state update queues with user group partitioning
    private static final String STATE_UPDATE_HIGH_QUEUE = "queue:state_update:high";
    private static final String STATE_UPDATE_NORMAL_QUEUE = "queue:state_update:normal";
    private static final String STATE_UPDATE_LOW_QUEUE = "queue:state_update:low";
    private static final String STATE_UPDATE_BATCH_QUEUE = "queue:state_update:batch";
    
    // User group partitioned queues
    private static final String USER_GROUP_PREFIX = "queue:user_group:";
    private static final String DEAD_LETTER_QUEUE = "queue:dead_letter";
    private static final String RETRY_QUEUE = "queue:retry";

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
                
                // Route request to specific instance
                return routeRequestToInstance(request, selectedInstance);
                
            } catch (Exception e) {
                log.error("Error in load balancing: {}", e.getMessage(), e);
                // Fallback to queuing
                return queueRequest(request).join();
            }
        });
    }

    /**
     * Queue psychological state update with priority and user group partitioning
     */
    public CompletableFuture<String> queueStateUpdate(StateUpdateRequest stateUpdateRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PerformanceMonitor.start("state_update_queuing");
                
                // Generate request ID
                String requestId = generateRequestId();
                stateUpdateRequest.setRequestId(requestId);
                stateUpdateRequest.setQueuedAt(LocalDateTime.now());
                stateUpdateRequest.setStatus(StateUpdateStatus.QUEUED);
                
                // Determine queue based on priority and user group
                String queueName = determineStateUpdateQueue(stateUpdateRequest);
                
                // Add to appropriate queue with priority score
                double priorityScore = calculateStateUpdatePriorityScore(stateUpdateRequest);
                redisTemplate.opsForZSet().add(queueName, stateUpdateRequest, priorityScore);
                
                // Set TTL for request (2 hours for state updates)
                redisTemplate.expire(requestId, 7200, TimeUnit.SECONDS);
                
                // Update metrics
                totalQueuedRequests.incrementAndGet();
                
                PerformanceMonitor.end("state_update_queuing");
                log.debug("State update {} queued in {} with priority {}", 
                    requestId, queueName, priorityScore);
                
                return requestId;
                
            } catch (Exception e) {
                log.error("Error queuing state update: {}", e.getMessage(), e);
                throw new RuntimeException("State update queuing failed", e);
            }
        });
    }

    /**
     * Process psychological state updates from queue with batch processing
     */
    public CompletableFuture<List<StateUpdateRequest>> processStateUpdateBatch(String queueName, int batchSize) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PerformanceMonitor.start("state_update_batch_processing");
                
                // Get batch of highest priority requests from queue
                Set<Object> requests = redisTemplate.opsForZSet().range(queueName, 0, batchSize - 1);
                
                if (requests.isEmpty()) {
                    return new ArrayList<>(); // No requests in queue
                }
                
                List<StateUpdateRequest> batch = new ArrayList<>();
                for (Object requestObj : requests) {
                    StateUpdateRequest request = (StateUpdateRequest) requestObj;
                    
                    // Check if system can handle the request
                    if (concurrencyManager.canHandleRequest(request.getUserId())) {
                        // Remove from queue
                        redisTemplate.opsForZSet().remove(queueName, request);
                        
                        // Update request status
                        request.setStatus(StateUpdateStatus.PROCESSING);
                        request.setProcessingStartedAt(LocalDateTime.now());
                        
                        batch.add(request);
                    } else {
                        // Move to back of queue with lower priority
                        redisTemplate.opsForZSet().remove(queueName, request);
                        redisTemplate.opsForZSet().add(queueName, request, 
                            calculateStateUpdatePriorityScore(request) - 1);
                    }
                }
                
                // Update metrics
                totalProcessedRequests.addAndGet(batch.size());
                
                PerformanceMonitor.end("state_update_batch_processing");
                log.debug("Processing batch of {} state updates from queue {}", batch.size(), queueName);
                
                return batch;
                
            } catch (Exception e) {
                log.error("Error processing state update batch from queue {}: {}", queueName, e.getMessage(), e);
                totalFailedRequests.incrementAndGet();
                return new ArrayList<>();
            }
        });
    }

    /**
     * Handle failed state update with retry logic
     */
    public CompletableFuture<Void> handleFailedStateUpdate(StateUpdateRequest request, Exception error) {
        return CompletableFuture.runAsync(() -> {
            try {
                request.setRetryCount(request.getRetryCount() + 1);
                request.setErrorMessage(error.getMessage());
                
                if (request.getRetryCount() <= 3) {
                    // Move to retry queue with exponential backoff
                    long delaySeconds = (long) Math.pow(2, request.getRetryCount()) * 60; // 2, 4, 8 minutes
                    request.setRetryAfter(LocalDateTime.now().plusSeconds(delaySeconds));
                    
                    redisTemplate.opsForZSet().add(RETRY_QUEUE, request, 
                        System.currentTimeMillis() + delaySeconds * 1000);
                    
                    log.warn("State update {} moved to retry queue (attempt {})", 
                        request.getRequestId(), request.getRetryCount());
                } else {
                    // Move to dead letter queue
                    request.setStatus(StateUpdateStatus.FAILED);
                    redisTemplate.opsForZSet().add(DEAD_LETTER_QUEUE, request, 
                        System.currentTimeMillis());
                    
                    log.error("State update {} moved to dead letter queue after {} retries", 
                        request.getRequestId(), request.getRetryCount());
                }
                
            } catch (Exception e) {
                log.error("Error handling failed state update {}: {}", request.getRequestId(), e.getMessage(), e);
            }
        });
    }

    /**
     * Get queue health status for monitoring
     */
    public QueueHealthStatus getQueueHealthStatus() {
        try {
            Map<String, Long> queueSizes = new HashMap<>();
            Map<String, Double> queueDepths = new HashMap<>();
            
            // Get all queue sizes
            queueSizes.put("high_priority", redisTemplate.opsForZSet().size(HIGH_PRIORITY_QUEUE));
            queueSizes.put("normal_priority", redisTemplate.opsForZSet().size(NORMAL_PRIORITY_QUEUE));
            queueSizes.put("low_priority", redisTemplate.opsForZSet().size(LOW_PRIORITY_QUEUE));
            queueSizes.put("state_update_high", redisTemplate.opsForZSet().size(STATE_UPDATE_HIGH_QUEUE));
            queueSizes.put("state_update_normal", redisTemplate.opsForZSet().size(STATE_UPDATE_NORMAL_QUEUE));
            queueSizes.put("state_update_low", redisTemplate.opsForZSet().size(STATE_UPDATE_LOW_QUEUE));
            queueSizes.put("retry", redisTemplate.opsForZSet().size(RETRY_QUEUE));
            queueSizes.put("dead_letter", redisTemplate.opsForZSet().size(DEAD_LETTER_QUEUE));
            
            // Calculate queue depths (percentage of capacity)
            long totalCapacity = 10000; // Total queue capacity
            for (Map.Entry<String, Long> entry : queueSizes.entrySet()) {
                queueDepths.put(entry.getKey(), (double) entry.getValue() / totalCapacity * 100);
            }
            
            // Calculate overall health score
            double healthScore = calculateHealthScore(queueSizes, queueDepths);
            
            return QueueHealthStatus.builder()
                .overallHealth(healthScore)
                .queueSizes(queueSizes)
                .queueDepths(queueDepths)
                .totalQueuedRequests(totalQueuedRequests.get())
                .totalProcessedRequests(totalProcessedRequests.get())
                .totalFailedRequests(totalFailedRequests.get())
                .averageProcessingTime(calculateAverageProcessingTime())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting queue health status: {}", e.getMessage());
            return QueueHealthStatus.builder()
                .overallHealth(0.0)
                .queueSizes(new HashMap<>())
                .queueDepths(new HashMap<>())
                .totalQueuedRequests(0)
                .totalProcessedRequests(0)
                .totalFailedRequests(0)
                .averageProcessingTime(0.0)
                .timestamp(LocalDateTime.now())
                .build();
        }
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

    private String determineStateUpdateQueue(StateUpdateRequest request) {
        // Determine queue based on priority and user group
        String baseQueue;
        switch (request.getPriority()) {
            case HIGH:
                baseQueue = STATE_UPDATE_HIGH_QUEUE;
                break;
            case LOW:
                baseQueue = STATE_UPDATE_LOW_QUEUE;
                break;
            case BATCH:
                baseQueue = STATE_UPDATE_BATCH_QUEUE;
                break;
            default:
                baseQueue = STATE_UPDATE_NORMAL_QUEUE;
                break;
        }
        
        // Add user group partitioning if specified
        if (request.getUserGroup() != null && !request.getUserGroup().isEmpty()) {
            return USER_GROUP_PREFIX + request.getUserGroup() + ":" + baseQueue.replace("queue:state_update:", "");
        }
        
        return baseQueue;
    }

    private double calculateStateUpdatePriorityScore(StateUpdateRequest request) {
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
        
        // Adjust based on state update urgency
        if (request.getUrgency() != null) {
            baseScore += request.getUrgency().getScore();
        }
        
        return Math.max(0, baseScore);
    }

    private double calculateHealthScore(Map<String, Long> queueSizes, Map<String, Double> queueDepths) {
        double healthScore = 1.0;
        
        // Penalize high queue depths
        for (Double depth : queueDepths.values()) {
            if (depth > 80) {
                healthScore -= 0.3; // Critical
            } else if (depth > 60) {
                healthScore -= 0.2; // Warning
            } else if (depth > 40) {
                healthScore -= 0.1; // Caution
            }
        }
        
        // Penalize high failure rate
        long totalRequests = totalQueuedRequests.get();
        long failedRequests = totalFailedRequests.get();
        if (totalRequests > 0) {
            double failureRate = (double) failedRequests / totalRequests;
            if (failureRate > 0.1) {
                healthScore -= 0.2; // High failure rate
            } else if (failureRate > 0.05) {
                healthScore -= 0.1; // Moderate failure rate
            }
        }
        
        return Math.max(0.0, healthScore);
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

    // Psychological State Update specific enums and classes
    public enum StateUpdatePriority {
        HIGH(10.0),
        NORMAL(5.0),
        LOW(1.0),
        BATCH(3.0);

        private final double score;

        StateUpdatePriority(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

    public enum StateUpdateStatus {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRYING,
        DEAD_LETTER
    }

    public enum StateUpdateUrgency {
        CRITICAL(5.0),
        HIGH(3.0),
        NORMAL(1.0),
        LOW(0.5);

        private final double score;

        StateUpdateUrgency(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class StateUpdateRequest {
        private String requestId;
        private String userId;
        private String userGroup;
        private StateUpdateType updateType;
        private Object updateData;
        private StateUpdatePriority priority;
        private StateUpdateUrgency urgency;
        private UserTier userTier;
        private StateUpdateStatus status;
        private LocalDateTime queuedAt;
        private LocalDateTime processingStartedAt;
        private LocalDateTime completedAt;
        private LocalDateTime retryAfter;
        private Object result;
        private int retryCount;
        private String errorMessage;
        private Long processingTimeMs;
        private Map<String, Object> metadata;
    }

    public enum StateUpdateType {
        POST_CREATION,
        INTERACTION_RESONANCE,
        INTERACTION_LINGER,
        PERSONALITY_UPDATE,
        MOOD_ANALYSIS,
        BATCH_UPDATE,
        EMERGENCY_UPDATE
    }

    @lombok.Data
    @lombok.Builder
    public static class QueueHealthStatus {
        private double overallHealth;
        private Map<String, Long> queueSizes;
        private Map<String, Double> queueDepths;
        private long totalQueuedRequests;
        private long totalProcessedRequests;
        private long totalFailedRequests;
        private double averageProcessingTime;
        private LocalDateTime timestamp;
    }
}
