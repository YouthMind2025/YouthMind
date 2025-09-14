package ai.youthmind.service;

import ai.youthmind.dto.AIChatRequest;
import ai.youthmind.dto.DeepSeekModerationRequest;
import ai.youthmind.dto.DeepSeekModerationResponse;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for batching AI requests to optimize performance and reduce API costs
 */
@Service
public class AIRequestBatchingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIRequestBatchingService.class);
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    // Batch configuration
    private static final int MAX_BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 2000; // 2 seconds
    private static final String BATCH_QUEUE_KEY = "ai_batch_queue";
    private static final String BATCH_RESULTS_KEY = "ai_batch_results";
    
    // In-memory batch management
    private final Map<String, BatchRequest> pendingBatches = new ConcurrentHashMap<>();
    private final AtomicInteger batchCounter = new AtomicInteger(0);
    
    /**
     * Process AI chat request with batching optimization
     */
    @Async
    public CompletableFuture<String> processBatchedAIRequest(AIChatRequest request, UUID userId) {
        try {
            // Check if we can add to existing batch
            String batchKey = findOrCreateBatch();
            
            // Add request to batch
            BatchRequest batchRequest = new BatchRequest();
            batchRequest.setRequestId(UUID.randomUUID().toString());
            batchRequest.setUserId(userId);
            batchRequest.setRequest(request);
            batchRequest.setTimestamp(LocalDateTime.now());
            batchRequest.setStatus(BatchRequest.Status.PENDING);
            
            // Store in Redis for persistence
            String requestKey = BATCH_QUEUE_KEY + ":" + batchKey + ":" + batchRequest.getRequestId();
            redisTemplate.opsForValue().set(requestKey, batchRequest, 5, TimeUnit.MINUTES);
            
            // Add to in-memory batch
            pendingBatches.computeIfAbsent(batchKey, k -> new BatchRequest()).addRequest(batchRequest);
            
            // Check if batch is ready to process
            BatchRequest batch = pendingBatches.get(batchKey);
            if (batch.getRequests().size() >= MAX_BATCH_SIZE || 
                (System.currentTimeMillis() - batch.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000) >= BATCH_TIMEOUT_MS) {
                
                // Process the batch
                processBatch(batchKey);
            }
            
            // Wait for result
            return waitForBatchResult(batchRequest.getRequestId());
            
        } catch (Exception e) {
            logger.error("Error processing batched AI request: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture("I'm sorry, I encountered an error. Please try again.");
        }
    }
    
    /**
     * Find existing batch or create new one
     */
    private String findOrCreateBatch() {
        // Look for existing batch with space
        for (Map.Entry<String, BatchRequest> entry : pendingBatches.entrySet()) {
            if (entry.getValue().getRequests().size() < MAX_BATCH_SIZE) {
                return entry.getKey();
            }
        }
        
        // Create new batch
        String batchKey = "batch_" + batchCounter.incrementAndGet() + "_" + System.currentTimeMillis();
        BatchRequest newBatch = new BatchRequest();
        newBatch.setBatchId(batchKey);
        newBatch.setCreatedAt(LocalDateTime.now());
        newBatch.setRequests(new ArrayList<>());
        pendingBatches.put(batchKey, newBatch);
        
        return batchKey;
    }
    
    /**
     * Process a batch of AI requests
     */
    @Async
    public void processBatch(String batchKey) {
        try {
            BatchRequest batch = pendingBatches.remove(batchKey);
            if (batch == null || batch.getRequests().isEmpty()) {
                return;
            }
            
            logger.info("Processing AI batch {} with {} requests", batchKey, batch.getRequests().size());
            
            // Group requests by personality type for better batching
            Map<String, List<BatchRequest>> groupedRequests = groupRequestsByPersonality(batch.getRequests());
            
            // Process each group
            for (Map.Entry<String, List<BatchRequest>> entry : groupedRequests.entrySet()) {
                processBatchGroup(entry.getKey(), entry.getValue());
            }
            
        } catch (Exception e) {
            logger.error("Error processing batch {}: {}", batchKey, e.getMessage(), e);
            // Mark all requests in batch as failed
            markBatchAsFailed(batchKey);
        }
    }
    
    /**
     * Group requests by personality type for optimized processing
     */
    private Map<String, List<BatchRequest>> groupRequestsByPersonality(List<BatchRequest> requests) {
        Map<String, List<BatchRequest>> grouped = new HashMap<>();
        
        for (BatchRequest request : requests) {
            try {
                // Get user personality profile from UserPersonalityService
                UserPersonalityProfile personalityProfile = userPersonalityService.getUserPersonalityProfile(request.getUserId());
                String personality = personalityProfile.getPreferredPersonalityType();
                
                // Fallback to default if personality is null or empty
                if (personality == null || personality.trim().isEmpty()) {
                    personality = "supportive";
                }
                
                grouped.computeIfAbsent(personality, k -> new ArrayList<>()).add(request);
                
                logger.debug("Grouped request {} for user {} under personality: {}", 
                    request.getRequestId(), request.getUserId(), personality);
                
            } catch (Exception e) {
                logger.warn("Failed to get personality profile for user {}: {}, using default", 
                    request.getUserId(), e.getMessage());
                
                // Use default personality on error
                grouped.computeIfAbsent("supportive", k -> new ArrayList<>()).add(request);
            }
        }
        
        // Log grouping summary
        logger.info("Grouped {} requests by personality: {}", 
            requests.size(), 
            grouped.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().size())
                .toList());
        
        return grouped;
    }
    
    /**
     * Process a group of requests with the same personality
     */
    private void processBatchGroup(String personality, List<BatchRequest> requests) {
        try {
            // Get personality profile for the first user to use as template
            UserPersonalityProfile personalityProfile = null;
            if (!requests.isEmpty()) {
                try {
                    personalityProfile = userPersonalityService.getUserPersonalityProfile(requests.get(0).getUserId());
                } catch (Exception e) {
                    logger.warn("Failed to get personality profile for batch group: {}", e.getMessage());
                }
            }
            
            // Create batched API request with actual personality information
            DeepSeekModerationRequest batchedRequest = createBatchedAPIRequest(personality, requests, personalityProfile);
            
            // Call AI service
            DeepSeekModerationResponse response = aiServiceClient.generateChatCompletion(batchedRequest);
            
            // Parse and distribute responses
            distributeBatchResponses(requests, response);
            
        } catch (Exception e) {
            logger.error("Error processing batch group for personality {}: {}", personality, e.getMessage(), e);
            // Mark all requests in group as failed
            for (BatchRequest request : requests) {
                markRequestAsFailed(request);
            }
        }
    }
    
    /**
     * Create batched API request for multiple users
     */
    private DeepSeekModerationRequest createBatchedAPIRequest(String personality, List<BatchRequest> requests, UserPersonalityProfile personalityProfile) {
        StringBuilder systemPrompt = new StringBuilder();
        
        // Use personality-specific prompts from AIChatService
        Map<String, String> personalityPrompts = Map.of(
            "supportive", """
                You are a supportive AI companion for YouthMind, a mental health platform for youth.
                Your role is to provide empathetic, encouraging, and understanding responses.
                Always validate feelings, offer gentle guidance, and maintain a warm, caring tone.
                Focus on emotional support and practical coping strategies.
                """,
            "professional", """
                You are a professional AI counselor for YouthMind, a mental health platform for youth.
                Provide evidence-based guidance, maintain professional boundaries, and offer structured support.
                Use clinical language appropriately while remaining accessible to young people.
                Focus on therapeutic techniques and professional mental health resources.
                """,
            "casual", """
                You are a friendly AI peer for YouthMind, a mental health platform for youth.
                Communicate like a supportive friend who understands what they're going through.
                Use casual language, share relatable experiences, and maintain a conversational tone.
                Focus on peer support and shared understanding.
                """,
            "empathetic", """
                You are an empathetic AI companion for YouthMind, a mental health platform for youth.
                Deeply understand and reflect emotions, provide compassionate responses, and show genuine care.
                Use emotional intelligence to connect and provide comfort.
                Focus on emotional validation and deep understanding.
                """
        );
        
        // Get the appropriate personality prompt
        String basePrompt = personalityPrompts.getOrDefault(personality, personalityPrompts.get("supportive"));
        systemPrompt.append(basePrompt);
        systemPrompt.append("\n\nYou will receive multiple user messages and should respond to each one individually. ");
        systemPrompt.append("Format your response as: RESPONSE_1: [response to first message] RESPONSE_2: [response to second message] etc.\n\n");
        
        // Add personality profile information if available
        if (personalityProfile != null) {
            systemPrompt.append("User Profile Context:\n");
            systemPrompt.append("- Communication style: ").append(personalityProfile.getCommunicationStyle()).append("\n");
            systemPrompt.append("- Emotional state: ").append(personalityProfile.getEmotionalState()).append("\n");
            systemPrompt.append("- Interests: ").append(String.join(", ", personalityProfile.getInterests())).append("\n");
            systemPrompt.append("- Enable humor: ").append(personalityProfile.isEnableHumor()).append("\n");
            systemPrompt.append("- Enable empathy: ").append(personalityProfile.isEnableEmpathy()).append("\n");
            systemPrompt.append("- Preferred response length: ").append(String.join(", ", personalityProfile.getPreferredResponseLength())).append("\n\n");
        }
        
        StringBuilder userMessages = new StringBuilder();
        for (int i = 0; i < requests.size(); i++) {
            userMessages.append("MESSAGE_").append(i + 1).append(": ").append(requests.get(i).getRequest().getMessage()).append("\n");
        }
        
        DeepSeekModerationRequest.Message systemMsg = new DeepSeekModerationRequest.Message("system", systemPrompt.toString());
        DeepSeekModerationRequest.Message userMsg = new DeepSeekModerationRequest.Message("user", userMessages.toString());
        
        return new DeepSeekModerationRequest(
            "deepseek-chat",
            Arrays.asList(systemMsg, userMsg),
            0.7,
            1000, // Increased tokens for multiple responses
            false
        );
    }
    
    /**
     * Distribute batch responses to individual requests
     */
    private void distributeBatchResponses(List<BatchRequest> requests, DeepSeekModerationResponse response) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            logger.warn("Empty response from batched AI request");
            for (BatchRequest request : requests) {
                markRequestAsFailed(request);
            }
            return;
        }
        
        String responseText = response.getChoices().get(0).getMessage().getContent();
        String[] responses = parseBatchedResponse(responseText, requests.size());
        
        for (int i = 0; i < requests.size() && i < responses.length; i++) {
            BatchRequest request = requests.get(i);
            request.setResponse(responses[i]);
            request.setStatus(BatchRequest.Status.COMPLETED);
            
            // Store result in Redis
            String resultKey = BATCH_RESULTS_KEY + ":" + request.getRequestId();
            redisTemplate.opsForValue().set(resultKey, request, 10, TimeUnit.MINUTES);
        }
    }
    
    /**
     * Parse batched response into individual responses
     */
    private String[] parseBatchedResponse(String responseText, int expectedCount) {
        String[] responses = new String[expectedCount];
        
        // Simple parsing - look for RESPONSE_1:, RESPONSE_2:, etc.
        for (int i = 0; i < expectedCount; i++) {
            String pattern = "RESPONSE_" + (i + 1) + ":";
            int startIndex = responseText.indexOf(pattern);
            
            if (startIndex != -1) {
                startIndex += pattern.length();
                int endIndex = responseText.indexOf("RESPONSE_" + (i + 2) + ":", startIndex);
                if (endIndex == -1) {
                    endIndex = responseText.length();
                }
                
                responses[i] = responseText.substring(startIndex, endIndex).trim();
            } else {
                // Fallback: split by lines and assign
                String[] lines = responseText.split("\n");
                if (i < lines.length) {
                    responses[i] = lines[i].trim();
                } else {
                    responses[i] = "I'm here to support you. How can I help?";
                }
            }
        }
        
        return responses;
    }
    
    /**
     * Wait for batch result
     */
    private CompletableFuture<String> waitForBatchResult(String requestId) {
        return CompletableFuture.supplyAsync(() -> {
            int maxWaitTime = 10000; // 10 seconds
            int waitInterval = 100; // 100ms
            int waited = 0;
            
            while (waited < maxWaitTime) {
                try {
                    String resultKey = BATCH_RESULTS_KEY + ":" + requestId;
                    Object result = redisTemplate.opsForValue().get(resultKey);
                    
                    if (result instanceof BatchRequest) {
                        BatchRequest batchRequest = (BatchRequest) result;
                        if (batchRequest.getStatus() == BatchRequest.Status.COMPLETED) {
                            return batchRequest.getResponse();
                        } else if (batchRequest.getStatus() == BatchRequest.Status.FAILED) {
                            return "I'm sorry, I encountered an error. Please try again.";
                        }
                    }
                    
                    Thread.sleep(waitInterval);
                    waited += waitInterval;
                    
                } catch (Exception e) {
                    logger.error("Error waiting for batch result: {}", e.getMessage());
                    return "I'm sorry, I encountered an error. Please try again.";
                }
            }
            
            return "I'm sorry, the request timed out. Please try again.";
        });
    }
    
    /**
     * Mark batch as failed
     */
    private void markBatchAsFailed(String batchKey) {
        try {
            logger.warn("Marking batch {} as failed", batchKey);
            
            // Get the batch from in-memory cache first
            BatchRequest batch = pendingBatches.get(batchKey);
            if (batch != null && batch.getRequests() != null) {
                // Mark all requests in the batch as failed
                for (BatchRequest request : batch.getRequests()) {
                    markRequestAsFailed(request);
                }
                
                // Remove from pending batches
                pendingBatches.remove(batchKey);
                logger.info("Marked {} requests in batch {} as failed", batch.getRequests().size(), batchKey);
            } else {
                // If batch not in memory, try to get from Redis
                markBatchAsFailedFromRedis(batchKey);
            }
            
        } catch (Exception e) {
            logger.error("Error marking batch {} as failed: {}", batchKey, e.getMessage(), e);
        }
    }
    
    /**
     * Mark batch as failed from Redis
     */
    private void markBatchAsFailedFromRedis(String batchKey) {
        try {
            // Get all request keys for this batch from Redis
            String pattern = BATCH_QUEUE_KEY + ":" + batchKey + ":*";
            Set<String> requestKeys = redisTemplate.keys(pattern);
            
            if (requestKeys != null && !requestKeys.isEmpty()) {
                for (String requestKey : requestKeys) {
                    try {
                        // Get the request from Redis
                        Object requestObj = redisTemplate.opsForValue().get(requestKey);
                        if (requestObj instanceof BatchRequest) {
                            BatchRequest request = (BatchRequest) requestObj;
                            markRequestAsFailed(request);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to mark individual request {} as failed: {}", requestKey, e.getMessage());
                    }
                }
                
                // Clean up the batch request keys from Redis
                redisTemplate.delete(requestKeys);
                logger.info("Marked {} requests in batch {} as failed from Redis", requestKeys.size(), batchKey);
            } else {
                logger.warn("No requests found for batch {} in Redis", batchKey);
            }
            
        } catch (Exception e) {
            logger.error("Error marking batch {} as failed from Redis: {}", batchKey, e.getMessage(), e);
        }
    }
    
    /**
     * Mark individual request as failed
     */
    private void markRequestAsFailed(BatchRequest request) {
        request.setStatus(BatchRequest.Status.FAILED);
        request.setResponse("I'm sorry, I encountered an error. Please try again.");
        
        // Store in Redis
        String resultKey = BATCH_RESULTS_KEY + ":" + request.getRequestId();
        redisTemplate.opsForValue().set(resultKey, request, 10, TimeUnit.MINUTES);
    }
    
    /**
     * Get batch statistics
     */
    public Map<String, Object> getBatchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingBatches", pendingBatches.size());
        stats.put("totalBatchesProcessed", batchCounter.get());
        stats.put("maxBatchSize", MAX_BATCH_SIZE);
        stats.put("batchTimeoutMs", BATCH_TIMEOUT_MS);
        return stats;
    }
    
    /**
     * Inner class for batch request management
     */
    public static class BatchRequest {
        private String batchId;
        private String requestId;
        private UUID userId;
        private AIChatRequest request;
        private String response;
        private Status status;
        private LocalDateTime timestamp;
        private LocalDateTime createdAt;
        private List<BatchRequest> requests;
        
        public enum Status {
            PENDING, PROCESSING, COMPLETED, FAILED
        }
        
        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public AIChatRequest getRequest() { return request; }
        public void setRequest(AIChatRequest request) { this.request = request; }
        
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public List<BatchRequest> getRequests() { return requests; }
        public void setRequests(List<BatchRequest> requests) { this.requests = requests; }
        
        public void addRequest(BatchRequest request) {
            if (this.requests == null) {
                this.requests = new ArrayList<>();
            }
            this.requests.add(request);
        }
    }
}
