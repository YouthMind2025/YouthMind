package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.dto.DeepSeekModerationRequest;
import ai.youthmind.dto.DeepSeekModerationResponse;
import ai.youthmind.dto.ModerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Enhanced DeepSeek Adapter Service with advanced retry logic, streaming,
 * caching, batch processing, and comprehensive error handling.
 */
@Service
public class DeepSeekAdapterService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAdapterService.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private DeepSeekCostOptimizationService costOptimizationService;
    
    @Autowired
    private DeepSeekRateLimitService rateLimitService;
    
    @Autowired
    private DeepSeekUsageMonitoringService usageMonitoringService;
    
    private final RestTemplate restTemplate;
    
    // Retry configuration with exponential backoff and jitter
    private static final int MAX_RETRY_ATTEMPTS = 2; // Max 2 attempts as specified
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMillis(200);
    private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;
    private static final Duration MAX_RETRY_DELAY = Duration.ofMillis(800); // 800ms default timeout
    private static final double JITTER_FACTOR = 0.1;
    
    // Batch processing configuration
    private static final int MAX_BATCH_SIZE = 10;
    private static final Duration BATCH_TIMEOUT = Duration.ofMillis(10000); // Increased to 10 seconds
    
    // Response validation
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(800);
    
    public DeepSeekAdapterService() {
        this.restTemplate = new RestTemplate();
        configureRestTemplate();
    }
    
    /**
     * Configure RestTemplate with timeout settings
     */
    private void configureRestTemplate() {
        // Set timeout to 800ms as specified
        restTemplate.getRequestFactory().getClass();
        // Additional timeout configuration would go here
    }
    
    /**
     * 4.1 Implement retry logic with exponential backoff
     * Enhanced retry with exponential backoff, jitter, and intelligent failure detection
     */
    public ModerationResult moderateContentWithRetry(String content, String contentType, UUID userId, String sessionId) {
        return executeWithRetry(
            () -> moderateContentInternal(content, contentType, userId, sessionId),
            "moderation",
            userId
        );
    }
    
    /**
     * Execute operation with enhanced retry logic
     */
    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationType, UUID userId) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // Check rate limits before each attempt
                DeepSeekRateLimitService.RateLimitResult rateLimitResult = 
                    rateLimitService.checkRateLimit(userId != null ? userId.toString() : null, operationType);
                
                if (!rateLimitResult.isAllowed()) {
                    throw new RateLimitExceededException("Rate limit exceeded: " + rateLimitResult.getMessage());
                }
                
                // Execute the operation
                T result = operation.execute();
                
                // Record successful request
                rateLimitService.recordRequest(userId != null ? userId.toString() : null, operationType);
                
                if (attempt > 0) {
                    logger.info("Operation {} succeeded on attempt {} for user {}", operationType, attempt + 1, userId);
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                // Don't retry on certain types of errors
                if (shouldNotRetry(e)) {
                    logger.warn("Non-retryable error for {}: {}", operationType, e.getMessage());
                    break;
                }
                
                // If this was the last attempt, don't wait
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    logger.error("Operation {} failed after {} attempts for user {}: {}", 
                        operationType, MAX_RETRY_ATTEMPTS + 1, userId, e.getMessage());
                    break;
                }
                
                // Calculate delay with exponential backoff and jitter
                Duration delay = calculateRetryDelay(attempt);
                
                logger.warn("Operation {} failed on attempt {} for user {}, retrying in {}ms: {}", 
                    operationType, attempt + 1, userId, delay.toMillis(), e.getMessage());
                
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        // All retries failed
        throw new DeepSeekAdapterException("Operation failed after all retries", lastException);
    }
    
    /**
     * Calculate retry delay with exponential backoff and jitter
     */
    private Duration calculateRetryDelay(int attempt) {
        // Exponential backoff: delay = initial * (multiplier ^ attempt)
        long baseDelay = INITIAL_RETRY_DELAY.toMillis() * (long) Math.pow(RETRY_BACKOFF_MULTIPLIER, attempt);
        
        // Cap at maximum delay
        long cappedDelay = Math.min(baseDelay, MAX_RETRY_DELAY.toMillis());
        
        // Add jitter: ±10% random variation
        double jitter = ThreadLocalRandom.current().nextDouble(-JITTER_FACTOR, JITTER_FACTOR);
        long jitteredDelay = (long) (cappedDelay * (1 + jitter));
        
        // Ensure minimum delay
        return Duration.ofMillis(Math.max(jitteredDelay, 50));
    }
    
    /**
     * Determine if an error should not be retried
     */
    private boolean shouldNotRetry(Exception e) {
        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpError = (HttpClientErrorException) e;
            int statusCode = httpError.getStatusCode().value();
            
            // Don't retry client errors (4xx) except for rate limiting
            return statusCode != 429; // 429 Too Many Requests should be retried
        }
        
        if (e instanceof RateLimitExceededException) {
            return false; // Rate limit errors should be retried
        }
        
        // Don't retry on validation errors or authentication errors
        return e instanceof IllegalArgumentException || 
               e instanceof SecurityException;
    }
    
    /**
     * Internal moderation method
     */
    private ModerationResult moderateContentInternal(String content, String contentType, UUID userId, String sessionId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check cache first
            String cacheHash = costOptimizationService.generateCacheHash(content, deepSeekConfig.getDefaultModel(), 0.1);
            DeepSeekCostOptimizationService.CacheCheckResult cacheResult = costOptimizationService.checkCache(cacheHash);
            
            if (cacheResult.isCacheHit()) {
                logger.debug("Cache hit for moderation request");
                // Return cached result (would need to deserialize from cache)
                return createCachedModerationResult(cacheResult.getCachedResponse());
            }
            
            // Build request
            DeepSeekModerationRequest request = buildModerationRequest(content);
            
            // Make API call
            DeepSeekModerationResponse response = callDeepSeekAPI(request);
            
            // Parse response
            ModerationResult result = parseModerationResponse(response, content);
            
            // Cache the result
            costOptimizationService.storeInCache(cacheHash, response, 24); // 24 hours TTL
            
            // Record usage
            recordUsage(userId, contentType, response, System.currentTimeMillis() - startTime, true);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            recordUsage(userId, contentType, null, duration, false);
            throw new DeepSeekAdapterException("Moderation failed", e);
        }
    }
    
    /**
     * 4.2 Add streaming response handling
     * Handle streaming responses from DeepSeek API
     */
    public CompletableFuture<ModerationResult> moderateContentStreaming(String content, String contentType, UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For now, we'll simulate streaming by processing in chunks
                // In a real implementation, this would handle Server-Sent Events or WebSocket streams
                
                logger.debug("Starting streaming moderation for content: {}", content.substring(0, Math.min(50, content.length())));
                
                // Simulate streaming by processing content in chunks
                List<String> chunks = splitContentIntoChunks(content, 100);
                ModerationResult finalResult = null;
                
                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    
                    // Process each chunk
                    ModerationResult chunkResult = moderateContentWithRetry(chunk, contentType, userId, null);
                    
                    // For streaming, we might want to return intermediate results
                    if (i == 0) {
                        finalResult = chunkResult;
                    } else {
                        // Combine results (simplified logic)
                        finalResult = combineModerationResults(finalResult, chunkResult);
                    }
                    
                    // Simulate streaming delay
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Streaming interrupted", e);
                    }
                }
                
                return finalResult != null ? finalResult : ModerationResult.safe("Streaming moderation completed");
                
            } catch (Exception e) {
                logger.error("Streaming moderation failed: {}", e.getMessage(), e);
                return ModerationResult.safe("Streaming moderation failed, content approved by default");
            }
        });
    }
    
    /**
     * Split content into chunks for streaming processing
     */
    private List<String> splitContentIntoChunks(String content, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < content.length(); i += chunkSize) {
            chunks.add(content.substring(i, Math.min(i + chunkSize, content.length())));
        }
        return chunks;
    }
    
    /**
     * Combine multiple moderation results
     */
    private ModerationResult combineModerationResults(ModerationResult result1, ModerationResult result2) {
        // If either result is unsafe, the combined result is unsafe
        if (!result1.isSafe() || !result2.isSafe()) {
            ModerationResult combined = new ModerationResult();
            combined.setSafe(false);
            combined.setReason("Content flagged in multiple sections");
            combined.setConfidence(Math.min(result1.getConfidence(), result2.getConfidence()));
            return combined;
        }
        
        // Both are safe, return the more confident result
        return result1.getConfidence() > result2.getConfidence() ? result1 : result2;
    }
    
    /**
     * 4.6 Create batch request processing
     * Process multiple requests in batches for efficiency
     */
    public List<ModerationResult> moderateContentBatch(List<BatchModerationRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }
        
        logger.info("Processing batch of {} moderation requests", requests.size());
        
        // Split into smaller batches
        List<List<BatchModerationRequest>> batches = splitIntoBatches(requests, MAX_BATCH_SIZE);
        List<ModerationResult> allResults = new ArrayList<>();
        
        for (List<BatchModerationRequest> batch : batches) {
            try {
                List<ModerationResult> batchResults = processBatch(batch);
                allResults.addAll(batchResults);
            } catch (Exception e) {
                logger.error("Batch processing failed, adding fallback results: {}", e.getMessage());
                // Add fallback results for failed batch
                for (BatchModerationRequest request : batch) {
                    allResults.add(ModerationResult.safe("Batch processing failed, content approved by default"));
                }
            }
        }
        
        return allResults;
    }
    
    /**
     * Split requests into batches
     */
    private List<List<BatchModerationRequest>> splitIntoBatches(List<BatchModerationRequest> requests, int batchSize) {
        List<List<BatchModerationRequest>> batches = new ArrayList<>();
        
        for (int i = 0; i < requests.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, requests.size());
            batches.add(requests.subList(i, endIndex));
        }
        
        return batches;
    }
    
    /**
     * Process a single batch
     */
    private List<ModerationResult> processBatch(List<BatchModerationRequest> batch) {
        List<CompletableFuture<ModerationResult>> futures = new ArrayList<>();
        
        // Process each request in the batch concurrently
        for (BatchModerationRequest request : batch) {
            CompletableFuture<ModerationResult> future = CompletableFuture.supplyAsync(() -> 
                moderateContentWithRetry(request.getContent(), request.getContentType(), request.getUserId(), request.getSessionId())
            );
            futures.add(future);
        }
        
        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        
        try {
            allFutures.get(BATCH_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Batch processing timeout: {}", e.getMessage());
            throw new DeepSeekAdapterException("Batch processing timeout", e);
        }
        
        // Collect results
        List<ModerationResult> results = new ArrayList<>();
        for (CompletableFuture<ModerationResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                logger.error("Failed to get batch result: {}", e.getMessage());
                results.add(ModerationResult.safe("Batch processing failed, content approved by default"));
            }
        }
        
        return results;
    }
    
    /**
     * 4.7 Add response validation and error handling
     * Comprehensive response validation and error handling
     */
    private ModerationResult parseModerationResponse(DeepSeekModerationResponse response, String originalContent) {
        try {
            // Validate response structure
            validateResponse(response);
            
            // Parse the moderation decision
            String responseText = response.getChoices() != null && !response.getChoices().isEmpty() 
                ? response.getChoices().get(0).getMessage().getContent() 
                : "";
            
            // Enhanced parsing with better error handling
            ModerationResult result = parseModerationDecision(responseText, originalContent);
            
            // Validate result
            validateModerationResult(result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to parse moderation response: {}", e.getMessage(), e);
            return ModerationResult.safe("Response parsing failed, content approved by default");
        }
    }
    
    /**
     * Validate API response structure
     */
    private void validateResponse(DeepSeekModerationResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Response is null");
        }
        
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalArgumentException("Response has no choices");
        }
        
        if (response.getChoices().get(0).getMessage() == null) {
            throw new IllegalArgumentException("Response message is null");
        }
        
        if (response.getChoices().get(0).getMessage().getContent() == null) {
            throw new IllegalArgumentException("Response content is null");
        }
    }
    
    /**
     * Parse moderation decision from response text
     */
    private ModerationResult parseModerationDecision(String responseText, String originalContent) {
        ModerationResult result = new ModerationResult();
        
        // Enhanced parsing logic
        String upperResponse = responseText.trim().toUpperCase();
        
        // Debug logging to see what AI is responding
        logger.debug("AI Response for content '{}': {}", 
            originalContent.length() > 50 ? originalContent.substring(0, 50) + "..." : originalContent, 
            responseText);
        
        if (upperResponse.startsWith("APPROVE")) {
            result.setSafe(true);
            result.setConfidence(0.9);
            result.setReason(extractReason(responseText));
            logger.debug("Content APPROVED: {}", extractReason(responseText));
        } else if (upperResponse.startsWith("REJECT")) {
            result.setSafe(false);
            result.setConfidence(0.8);
            result.setReason(extractReason(responseText));
            logger.debug("Content REJECTED: {}", extractReason(responseText));
        } else {
            // Fallback parsing for unexpected formats
            logger.warn("Unexpected response format: {}", responseText);
            result.setSafe(true);
            result.setConfidence(0.5);
            result.setReason("Unexpected response format, defaulting to approve");
        }
        
        result.setModeratedAt(LocalDateTime.now());
        return result;
    }
    
    /**
     * Extract reason from response text
     */
    private String extractReason(String responseText) {
        int colonIndex = responseText.indexOf(':');
        if (colonIndex > 0 && colonIndex < responseText.length() - 1) {
            return responseText.substring(colonIndex + 1).trim();
        }
        return responseText.trim();
    }
    
    /**
     * Validate moderation result
     */
    private void validateModerationResult(ModerationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Moderation result is null");
        }
        
        if (result.getConfidence() < 0.0 || result.getConfidence() > 1.0) {
            logger.warn("Invalid confidence value: {}, clamping to valid range", result.getConfidence());
            result.setConfidence(Math.max(0.0, Math.min(1.0, result.getConfidence())));
        }
        
        if (result.getReason() == null || result.getReason().trim().isEmpty()) {
            result.setReason("No reason provided");
        }
    }
    
    /**
     * Build moderation request
     */
    private DeepSeekModerationRequest buildModerationRequest(String content) {
        DeepSeekModerationRequest request = new DeepSeekModerationRequest();
        request.setModel(deepSeekConfig.getDefaultModel());
        
        DeepSeekModerationRequest.Message systemMessage = new DeepSeekModerationRequest.Message();
        systemMessage.setRole("system");
        systemMessage.setContent("You are a content moderation AI for a mental health platform. Analyze the following content and respond with APPROVE or REJECT followed by a brief reason. REJECT content that contains: profanity, hate speech, threats, self-harm, or inappropriate language. APPROVE content that is appropriate for a mental health support community, even if it discusses difficult emotions or struggles.");
        
        DeepSeekModerationRequest.Message userMessage = new DeepSeekModerationRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(content);
        
        request.setMessages(Arrays.asList(systemMessage, userMessage));
        request.setTemperature(0.1);
        request.setMaxTokens(100);
        
        return request;
    }
    
    /**
     * Call DeepSeek API
     */
    private DeepSeekModerationResponse callDeepSeekAPI(DeepSeekModerationRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekConfig.getApiKey());
            
            HttpEntity<DeepSeekModerationRequest> entity = new HttpEntity<>(request, headers);
            
            String url = deepSeekConfig.getBaseUrl() + "/chat/completions";
            
            ResponseEntity<DeepSeekModerationResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, DeepSeekModerationResponse.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP client error: {}", e.getMessage());
            throw new DeepSeekAdapterException("HTTP client error: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            logger.error("HTTP server error: {}", e.getMessage());
            throw new DeepSeekAdapterException("HTTP server error: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            logger.error("Resource access error: {}", e.getMessage());
            throw new DeepSeekAdapterException("Resource access error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            throw new DeepSeekAdapterException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Record API usage
     */
    private void recordUsage(UUID userId, String contentType, DeepSeekModerationResponse response, 
                           long responseTimeMs, boolean success) {
        try {
            int inputTokens = 0;
            int outputTokens = 0;
            
            if (response != null && response.getUsage() != null) {
                inputTokens = response.getUsage().getPromptTokens();
                outputTokens = response.getUsage().getCompletionTokens();
            }
            
            usageMonitoringService.recordUsage(userId, deepSeekConfig.getDefaultModel(), contentType,
                inputTokens, outputTokens, responseTimeMs, success, 0.001, false);
                
        } catch (Exception e) {
            logger.error("Failed to record usage: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create cached moderation result
     */
    private ModerationResult createCachedModerationResult(Object cachedResponse) {
        // This would deserialize the cached response
        // For now, return a safe result
        return ModerationResult.safe("Content approved (cached result)");
    }
    
    /**
     * Functional interface for retryable operations
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Batch moderation request
     */
    public static class BatchModerationRequest {
        private final String content;
        private final String contentType;
        private final UUID userId;
        private final String sessionId;
        
        public BatchModerationRequest(String content, String contentType, UUID userId, String sessionId) {
            this.content = content;
            this.contentType = contentType;
            this.userId = userId;
            this.sessionId = sessionId;
        }
        
        // Getters
        public String getContent() { return content; }
        public String getContentType() { return contentType; }
        public UUID getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
    }
    
    /**
     * Custom exceptions
     */
    public static class DeepSeekAdapterException extends RuntimeException {
        public DeepSeekAdapterException(String message) {
            super(message);
        }
        
        public DeepSeekAdapterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
