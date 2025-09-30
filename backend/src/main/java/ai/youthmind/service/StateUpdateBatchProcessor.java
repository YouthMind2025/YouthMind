package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * State Update Batch Processor for efficient processing of multiple state updates.
 * Provides sophisticated batch processing capabilities with priority handling,
 * load balancing, and performance optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StateUpdateBatchProcessor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RequestQueueService requestQueueService;

    // Batch processing parameters
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 1000;
    private static final int MIN_BATCH_SIZE = 10;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds

    // Performance tracking
    private final Map<String, BatchProcessingStats> batchStats = new ConcurrentHashMap<>();
    private final AtomicInteger totalBatchesProcessed = new AtomicInteger(0);
    private final AtomicInteger totalRequestsProcessed = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    /**
     * Process batch of state update requests
     */
    public CompletableFuture<BatchStateUpdateResult> processBatchUpdates(
            List<StateUpdateRequest> requests,
            Function<StateUpdateRequest, CompletableFuture<StateUpdateResult>> updateFunction) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String batchId = generateBatchId();
            
            try {
                log.info("Processing batch {} with {} requests", batchId, requests.size());
                
                // Validate batch size
                if (requests.isEmpty()) {
                    return BatchStateUpdateResult.builder()
                        .success(true)
                        .results(new ArrayList<>())
                        .successCount(0)
                        .failureCount(0)
                        .totalProcessingTime(0)
                        .build();
                }
                
                if (requests.size() > MAX_BATCH_SIZE) {
                    return BatchStateUpdateResult.builder()
                        .success(false)
                        .error("Batch size exceeds maximum limit: " + MAX_BATCH_SIZE)
                        .build();
                }
                
                // Group requests by priority
                Map<Priority, List<StateUpdateRequest>> groupedRequests = groupRequestsByPriority(requests);
                
                // Process requests in priority order
                List<StateUpdateResult> allResults = new ArrayList<>();
                int successCount = 0;
                int failureCount = 0;
                
                for (Priority priority : Priority.values()) {
                    List<StateUpdateRequest> priorityRequests = groupedRequests.get(priority);
                    if (priorityRequests != null && !priorityRequests.isEmpty()) {
                        List<StateUpdateResult> priorityResults = processPriorityBatch(
                            priorityRequests, updateFunction, batchId
                        ).get();
                        
                        allResults.addAll(priorityResults);
                        
                        for (StateUpdateResult result : priorityResults) {
                            if (result.isSuccess()) {
                                successCount++;
                            } else {
                                failureCount++;
                            }
                        }
                    }
                }
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Update batch statistics
                updateBatchStatistics(batchId, requests.size(), successCount, failureCount, processingTime);
                
                // Create result
                BatchStateUpdateResult result = BatchStateUpdateResult.builder()
                    .success(true)
                    .results(allResults)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .totalProcessingTime(processingTime)
                    .build();
                
                log.info("Batch {} completed: {} successes, {} failures, {}ms", 
                        batchId, successCount, failureCount, processingTime);
                
                return result;
                
            } catch (Exception e) {
                long processingTime = System.currentTimeMillis() - startTime;
                log.error("Error processing batch {}: {}", batchId, e.getMessage(), e);
                
                return BatchStateUpdateResult.builder()
                    .success(false)
                    .error("Batch processing failed: " + e.getMessage())
                    .totalProcessingTime(processingTime)
                    .build();
            }
        });
    }

    /**
     * Process requests with specific priority
     */
    private CompletableFuture<List<StateUpdateResult>> processPriorityBatch(
            List<StateUpdateRequest> requests,
            Function<StateUpdateRequest, CompletableFuture<StateUpdateResult>> updateFunction,
            String batchId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<StateUpdateResult> results = new ArrayList<>();
                
                // Process requests in parallel for better performance
                List<CompletableFuture<StateUpdateResult>> futures = new ArrayList<>();
                
                for (StateUpdateRequest request : requests) {
                    CompletableFuture<StateUpdateResult> future = updateFunction.apply(request)
                        .exceptionally(throwable -> {
                            log.error("Error processing request in batch {}: {}", batchId, throwable.getMessage());
                            return StateUpdateResult.builder()
                                .success(false)
                                .error("Request processing failed: " + throwable.getMessage())
                                .build();
                        });
                    futures.add(future);
                }
                
                // Wait for all requests to complete
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
                );
                
                allFutures.get(DEFAULT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                // Collect results
                for (CompletableFuture<StateUpdateResult> future : futures) {
                    results.add(future.get());
                }
                
                return results;
                
            } catch (Exception e) {
                log.error("Error processing priority batch {}: {}", batchId, e.getMessage(), e);
                return requests.stream()
                    .map(request -> StateUpdateResult.builder()
                        .success(false)
                        .error("Priority batch processing failed: " + e.getMessage())
                        .build())
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
        });
    }

    /**
     * Group requests by priority
     */
    private Map<Priority, List<StateUpdateRequest>> groupRequestsByPriority(List<StateUpdateRequest> requests) {
        Map<Priority, List<StateUpdateRequest>> grouped = new HashMap<>();
        
        for (Priority priority : Priority.values()) {
            grouped.put(priority, new ArrayList<>());
        }
        
        for (StateUpdateRequest request : requests) {
            Priority priority = determinePriority(request);
            grouped.get(priority).add(request);
        }
        
        return grouped;
    }

    /**
     * Determine priority for a request
     */
    private Priority determinePriority(StateUpdateRequest request) {
        // Priority determination logic based on request characteristics
        String observationType = request.getObservationType();
        Map<String, Object> context = request.getContext();
        
        // High priority for critical observations
        if ("crisis".equals(observationType) || "emergency".equals(observationType)) {
            return Priority.HIGH;
        }
        
        // Check context for high priority indicators
        if (context != null) {
            String emotionalState = (String) context.get("emotional_state");
            if ("high_stress".equals(emotionalState) || "crisis".equals(emotionalState)) {
                return Priority.HIGH;
            }
            
            Double confidence = (Double) context.get("confidence");
            if (confidence != null && confidence < 0.3) {
                return Priority.HIGH; // Low confidence = high priority for attention
            }
        }
        
        // Medium priority for important observations
        if ("emotional_state".equals(observationType) || "stress_level".equals(observationType)) {
            return Priority.MEDIUM;
        }
        
        // Default to low priority
        return Priority.LOW;
    }

    /**
     * Generate unique batch ID
     */
    private String generateBatchId() {
        return "batch_" + System.currentTimeMillis() + "_" + totalBatchesProcessed.incrementAndGet();
    }

    /**
     * Update batch processing statistics
     */
    private void updateBatchStatistics(String batchId, int totalRequests, int successCount, 
                                     int failureCount, long processingTime) {
        BatchProcessingStats stats = BatchProcessingStats.builder()
            .batchId(batchId)
            .totalRequests(totalRequests)
            .successCount(successCount)
            .failureCount(failureCount)
            .processingTimeMs(processingTime)
            .timestamp(System.currentTimeMillis())
            .build();
        
        batchStats.put(batchId, stats);
        totalRequestsProcessed.addAndGet(totalRequests);
        
        // Keep only recent batch stats (last 1000)
        if (batchStats.size() > 1000) {
            String oldestBatchId = batchStats.keySet().iterator().next();
            batchStats.remove(oldestBatchId);
        }
    }

    /**
     * Get batch processing statistics
     */
    public BatchProcessingSummary getBatchProcessingSummary() {
        Collection<BatchProcessingStats> allStats = batchStats.values();
        
        if (allStats.isEmpty()) {
            return BatchProcessingSummary.builder()
                .totalBatchesProcessed(0)
                .totalRequestsProcessed(0)
                .averageSuccessRate(0.0)
                .averageProcessingTimeMs(0.0)
                .build();
        }
        
        double totalSuccessRate = allStats.stream()
            .mapToDouble(stats -> (double) stats.getSuccessCount() / stats.getTotalRequests())
            .average()
            .orElse(0.0);
        
        double averageProcessingTime = allStats.stream()
            .mapToDouble(BatchProcessingStats::getProcessingTimeMs)
            .average()
            .orElse(0.0);
        
        return BatchProcessingSummary.builder()
            .totalBatchesProcessed(batchStats.size())
            .totalRequestsProcessed(totalRequestsProcessed.get())
            .averageSuccessRate(totalSuccessRate)
            .averageProcessingTimeMs(averageProcessingTime)
            .build();
    }

    /**
     * Clear batch processing statistics
     */
    public void clearBatchStatistics() {
        batchStats.clear();
        totalBatchesProcessed.set(0);
        totalRequestsProcessed.set(0);
    }

    /**
     * Process state updates from queue with dynamic batch sizing
     */
    public CompletableFuture<BatchStateUpdateResult> processQueuedStateUpdates(
            String queueName, 
            Function<RequestQueueService.StateUpdateRequest, CompletableFuture<StateUpdateResult>> updateFunction) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Determine optimal batch size based on queue depth and system load
                int optimalBatchSize = calculateOptimalBatchSize(queueName);
                
                // Get batch from queue
                List<RequestQueueService.StateUpdateRequest> queuedRequests = 
                    requestQueueService.processStateUpdateBatch(queueName, optimalBatchSize).join();
                
                if (queuedRequests.isEmpty()) {
                    return BatchStateUpdateResult.builder()
                        .success(true)
                        .results(new ArrayList<>())
                        .successCount(0)
                        .failureCount(0)
                        .totalProcessingTime(0)
                        .build();
                }
                
                // Convert to internal format and process
                List<StateUpdateRequest> internalRequests = convertToInternalFormat(queuedRequests);
                
                return processBatchUpdates(internalRequests, request -> {
                    // Convert back to queue format for processing
                    RequestQueueService.StateUpdateRequest queueRequest = 
                        convertToQueueFormat(request, queuedRequests);
                    return updateFunction.apply(queueRequest);
                }).join();
                
            } catch (Exception e) {
                log.error("Error processing queued state updates from {}: {}", queueName, e.getMessage(), e);
                return BatchStateUpdateResult.builder()
                    .success(false)
                    .error("Queue processing failed: " + e.getMessage())
                    .build();
            }
        });
    }

    /**
     * Process state updates with user group partitioning
     */
    public CompletableFuture<Map<String, BatchStateUpdateResult>> processStateUpdatesByUserGroup(
            Map<String, List<StateUpdateRequest>> userGroupRequests,
            Function<StateUpdateRequest, CompletableFuture<StateUpdateResult>> updateFunction) {
        
        return CompletableFuture.supplyAsync(() -> {
            Map<String, BatchStateUpdateResult> results = new HashMap<>();
            
            // Process each user group in parallel
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (Map.Entry<String, List<StateUpdateRequest>> entry : userGroupRequests.entrySet()) {
                String userGroup = entry.getKey();
                List<StateUpdateRequest> requests = entry.getValue();
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        BatchStateUpdateResult result = processBatchUpdates(requests, updateFunction).join();
                        results.put(userGroup, result);
                        
                        log.debug("Processed {} requests for user group {}", requests.size(), userGroup);
                        
                    } catch (Exception e) {
                        log.error("Error processing user group {}: {}", userGroup, e.getMessage(), e);
                        results.put(userGroup, BatchStateUpdateResult.builder()
                            .success(false)
                            .error("User group processing failed: " + e.getMessage())
                            .build());
                    }
                });
                
                futures.add(future);
            }
            
            // Wait for all user groups to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            return results;
        });
    }

    /**
     * Process state updates with priority-based scheduling
     */
    public CompletableFuture<BatchStateUpdateResult> processStateUpdatesWithPriority(
            List<StateUpdateRequest> requests,
            Function<StateUpdateRequest, CompletableFuture<StateUpdateResult>> updateFunction) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Sort requests by priority and urgency
                List<StateUpdateRequest> sortedRequests = requests.stream()
                    .sorted((r1, r2) -> {
                        // Sort by priority first, then by urgency
                        int priorityCompare = r1.getPriority().compareTo(r2.getPriority());
                        if (priorityCompare != 0) {
                            return priorityCompare;
                        }
                        return r1.getUrgency().compareTo(r2.getUrgency());
                    })
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                
                // Process in priority order with adaptive batch sizing
                List<StateUpdateResult> allResults = new ArrayList<>();
                int successCount = 0;
                int failureCount = 0;
                long totalProcessingTime = 0;
                
                for (StateUpdateRequest request : sortedRequests) {
                    long startTime = System.currentTimeMillis();
                    
                    try {
                        StateUpdateResult result = updateFunction.apply(request).get();
                        allResults.add(result);
                        
                        if (result.isSuccess()) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                        
                    } catch (Exception e) {
                        log.error("Error processing priority request: {}", e.getMessage(), e);
                        allResults.add(StateUpdateResult.builder()
                            .success(false)
                            .error("Priority processing failed: " + e.getMessage())
                            .build());
                        failureCount++;
                    }
                    
                    totalProcessingTime += System.currentTimeMillis() - startTime;
                }
                
                return BatchStateUpdateResult.builder()
                    .success(true)
                    .results(allResults)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .totalProcessingTime(totalProcessingTime)
                    .build();
                
            } catch (Exception e) {
                log.error("Error in priority-based processing: {}", e.getMessage(), e);
                return BatchStateUpdateResult.builder()
                    .success(false)
                    .error("Priority processing failed: " + e.getMessage())
                    .build();
            }
        });
    }

    /**
     * Get batch processing health status
     */
    public BatchProcessingHealthStatus getBatchProcessingHealthStatus() {
        try {
            Collection<BatchProcessingStats> allStats = batchStats.values();
            
            if (allStats.isEmpty()) {
                return BatchProcessingHealthStatus.builder()
                    .overallHealth(1.0)
                    .totalBatchesProcessed(0)
                    .totalRequestsProcessed(0)
                    .averageSuccessRate(0.0)
                    .averageProcessingTimeMs(0.0)
                    .queueIntegrationHealth(1.0)
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Calculate health metrics
            double averageSuccessRate = allStats.stream()
                .mapToDouble(stats -> (double) stats.getSuccessCount() / stats.getTotalRequests())
                .average()
                .orElse(0.0);
            
            double averageProcessingTime = allStats.stream()
                .mapToDouble(BatchProcessingStats::getProcessingTimeMs)
                .average()
                .orElse(0.0);
            
            // Calculate overall health score
            double healthScore = calculateHealthScore(averageSuccessRate, averageProcessingTime);
            
            // Get queue integration health
            double queueIntegrationHealth = getQueueIntegrationHealth();
            
            return BatchProcessingHealthStatus.builder()
                .overallHealth(healthScore)
                .totalBatchesProcessed(batchStats.size())
                .totalRequestsProcessed(totalRequestsProcessed.get())
                .averageSuccessRate(averageSuccessRate)
                .averageProcessingTimeMs(averageProcessingTime)
                .queueIntegrationHealth(queueIntegrationHealth)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting batch processing health status: {}", e.getMessage());
            return BatchProcessingHealthStatus.builder()
                .overallHealth(0.0)
                .totalBatchesProcessed(0)
                .totalRequestsProcessed(0)
                .averageSuccessRate(0.0)
                .averageProcessingTimeMs(0.0)
                .queueIntegrationHealth(0.0)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    // Private helper methods
    private int calculateOptimalBatchSize(String queueName) {
        try {
            Long queueSize = redisTemplate.opsForZSet().size(queueName);
            if (queueSize == null || queueSize == 0) {
                return MIN_BATCH_SIZE;
            }
            
            // Adaptive batch sizing based on queue depth
            if (queueSize < 50) {
                return MIN_BATCH_SIZE;
            } else if (queueSize < 200) {
                return DEFAULT_BATCH_SIZE;
            } else if (queueSize < 500) {
                return DEFAULT_BATCH_SIZE * 2;
            } else {
                return Math.min(MAX_BATCH_SIZE, DEFAULT_BATCH_SIZE * 3);
            }
            
        } catch (Exception e) {
            log.warn("Error calculating optimal batch size: {}", e.getMessage());
            return DEFAULT_BATCH_SIZE;
        }
    }

    private List<StateUpdateRequest> convertToInternalFormat(List<RequestQueueService.StateUpdateRequest> queueRequests) {
        return queueRequests.stream()
            .map(this::convertToInternalFormat)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private StateUpdateRequest convertToInternalFormat(RequestQueueService.StateUpdateRequest queueRequest) {
        return StateUpdateRequest.builder()
            .userId(queueRequest.getUserId())
            .currentState(extractCurrentState(queueRequest))
            .newObservation(extractNewObservation(queueRequest))
            .observationType(queueRequest.getUpdateType().name())
            .context(queueRequest.getMetadata())
            .build();
    }

    private RequestQueueService.StateUpdateRequest convertToQueueFormat(
            StateUpdateRequest internalRequest, 
            List<RequestQueueService.StateUpdateRequest> originalRequests) {
        
        // Find matching original request
        return originalRequests.stream()
            .filter(req -> req.getUserId().equals(internalRequest.getUserId()))
            .findFirst()
            .orElse(null);
    }

    private List<Float> extractCurrentState(RequestQueueService.StateUpdateRequest request) {
        // Extract current state from request data
        if (request.getUpdateData() instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) request.getUpdateData();
            Object stateObj = data.get("currentState");
            if (stateObj instanceof List) {
                return (List<Float>) stateObj;
            }
        }
        return new ArrayList<>();
    }

    private List<Float> extractNewObservation(RequestQueueService.StateUpdateRequest request) {
        // Extract new observation from request data
        if (request.getUpdateData() instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) request.getUpdateData();
            Object observationObj = data.get("newObservation");
            if (observationObj instanceof List) {
                return (List<Float>) observationObj;
            }
        }
        return new ArrayList<>();
    }

    private double calculateHealthScore(double successRate, double avgProcessingTime) {
        double healthScore = 1.0;
        
        // Penalize low success rate
        if (successRate < 0.8) {
            healthScore -= 0.3;
        } else if (successRate < 0.9) {
            healthScore -= 0.1;
        }
        
        // Penalize slow processing
        if (avgProcessingTime > 10000) { // 10 seconds
            healthScore -= 0.2;
        } else if (avgProcessingTime > 5000) { // 5 seconds
            healthScore -= 0.1;
        }
        
        return Math.max(0.0, healthScore);
    }

    private double getQueueIntegrationHealth() {
        try {
            // Check queue service health
            RequestQueueService.QueueHealthStatus queueHealth = requestQueueService.getQueueHealthStatus();
            return queueHealth.getOverallHealth();
            
        } catch (Exception e) {
            log.warn("Error getting queue integration health: {}", e.getMessage());
            return 0.5; // Assume moderate health if we can't determine
        }
    }

    // Enums and DTOs
    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    @lombok.Data
    @lombok.Builder
    public static class StateUpdateRequest {
        private String userId;
        private List<Float> currentState;
        private List<Float> newObservation;
        private String observationType;
        private Map<String, Object> context;
        private Priority priority;
        private Urgency urgency;
    }

    public enum Urgency {
        CRITICAL, HIGH, NORMAL, LOW
    }

    @lombok.Data
    @lombok.Builder
    public static class StateUpdateResult {
        private boolean success;
        private List<Float> updatedState;
        private double alpha;
        private StateSimilarityMetrics similarityMetrics;
        private double convergenceScore;
        private long processingTime;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchStateUpdateResult {
        private boolean success;
        private List<StateUpdateResult> results;
        private int successCount;
        private int failureCount;
        private long totalProcessingTime;
        private String error;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchProcessingStats {
        private String batchId;
        private int totalRequests;
        private int successCount;
        private int failureCount;
        private long processingTimeMs;
        private long timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchProcessingSummary {
        private int totalBatchesProcessed;
        private int totalRequestsProcessed;
        private double averageSuccessRate;
        private double averageProcessingTimeMs;
    }

    @lombok.Data
    @lombok.Builder
    public static class StateSimilarityMetrics {
        private double cosineSimilarity;
        private double euclideanDistance;
        private double manhattanDistance;
        private double jaccardSimilarity;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchProcessingHealthStatus {
        private double overallHealth;
        private int totalBatchesProcessed;
        private int totalRequestsProcessed;
        private double averageSuccessRate;
        private double averageProcessingTimeMs;
        private double queueIntegrationHealth;
        private LocalDateTime timestamp;
    }
}
