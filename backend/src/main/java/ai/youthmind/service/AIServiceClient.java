package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.config.ModerationConfig;
import ai.youthmind.dto.ApiUsageRequest;
import ai.youthmind.dto.DeepSeekModerationRequest;
import ai.youthmind.dto.DeepSeekModerationResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.AiApiUsage;
import ai.youthmind.exception.AIServiceException;
import ai.youthmind.util.CircuitBreaker;
import ai.youthmind.util.ModerationResponseParser;
import ai.youthmind.util.RetryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Service client for DeepSeek API integration
 * Handles content moderation and AI-powered content analysis
 */
@Service
public class AIServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AIServiceClient.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private ModerationConfig moderationConfig;
    
    @Autowired
    private ApiUsageMonitoringService usageMonitoringService;
    
    @Autowired
    private ApiCostCalculationService costCalculationService;
    
    @Autowired
    private AIFallbackService fallbackService;
    
    @Autowired
    @Qualifier("aiServiceCircuitBreaker")
    private CircuitBreaker circuitBreaker;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMillis(1000);
    private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;
    private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(10);
    private static final double JITTER_FACTOR = 0.1;
    
    // Moderation prompt templates
    private static final String MODERATION_SYSTEM_PROMPT = """
        You are a content moderation AI for YouthMind, a mental health support platform for youth.
        Your task is to analyze text content and determine if it's safe to publish.
        
        Guidelines:
        - APPROVE content that is supportive, educational, or shares personal experiences constructively
        - REJECT content that contains harassment, hate speech, violence, self-harm, or spam
        - Be especially careful with content that could harm vulnerable youth
        
        Respond with ONLY "APPROVE" or "REJECT" followed by a brief reason.
        Format: "APPROVE: [reason]" or "REJECT: [reason]"
        """;
    
    
    public AIServiceClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        // Configure timeout
        restTemplate.getRequestFactory().getClass();
    }
    
    /**
     * Moderate content using DeepSeek API
     * 
     * @param content The content to moderate
     * @param contentType Type of content (post, comment, etc.)
     * @param userId User ID for usage tracking (optional)
     * @param sessionId Session ID for usage tracking (optional)
     * @return ModerationResult with decision and details
     */
    public ModerationResult moderateContent(String content, String contentType, UUID userId, String sessionId) {
        // Validate configuration
        if (!isConfigured()) {
            logger.warn("DeepSeek API is not properly configured, using fallback");
            return fallbackService.generateFallbackModerationResult(content, contentType);
        }
        
        if (!moderationConfig.isEnabled()) {
            logger.debug("Moderation is disabled, approving content by default");
            return ModerationResult.safe("Moderation disabled");
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        final int[] promptTokens = {0};
        final int[] completionTokens = {0};
        
        try {
            logger.debug("Moderating {} content: {}", contentType, content.substring(0, Math.min(100, content.length())));
            
            // Use circuit breaker with retry logic
            ModerationResult result = circuitBreaker.execute(() -> {
                return RetryUtil.executeWithRetry(
                    () -> {
                        DeepSeekModerationRequest request = buildModerationRequest(content);
                        DeepSeekModerationResponse response = callDeepSeekAPIWithRetry(request);
                        
                        // Extract usage information
                        if (response.getUsage() != null) {
                            promptTokens[0] = response.getUsage().getPromptTokens();
                            completionTokens[0] = response.getUsage().getCompletionTokens();
                        }
                        
                        return parseModerationResponse(response, content);
                    },
                    MAX_RETRY_ATTEMPTS,
                    createAIServiceRetryPredicate(),
                    INITIAL_RETRY_DELAY,
                    RETRY_BACKOFF_MULTIPLIER,
                    MAX_RETRY_DELAY,
                    JITTER_FACTOR
                );
            });
            
            // Record successful usage
            recordApiUsage(userId, sessionId, AiApiUsage.RequestType.MODERATION, 
                          deepSeekConfig.getModel(), promptTokens[0], completionTokens[0], 
                          System.currentTimeMillis() - startTime, success, errorMessage, content);
            
            return result;
            
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            logger.warn("Circuit breaker is open, using fallback moderation: {}", e.getMessage());
            return fallbackService.generateFallbackModerationResult(content, contentType);
        } catch (RetryUtil.RetryExhaustedException e) {
            logger.error("All retry attempts exhausted, using fallback moderation: {}", e.getMessage());
            return fallbackService.generateFallbackModerationResult(content, contentType);
        } catch (AIServiceException e) {
            success = false;
            errorMessage = e.getMessage();
            
            // Record failed usage
            recordApiUsage(userId, sessionId, AiApiUsage.RequestType.MODERATION, 
                          deepSeekConfig.getModel(), promptTokens[0], completionTokens[0], 
                          System.currentTimeMillis() - startTime, success, errorMessage, content);
            
            logger.error("AI service error, using fallback moderation: {}", e.getMessage());
            return fallbackService.generateFallbackModerationResult(content, contentType);
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            
            // Record failed usage
            recordApiUsage(userId, sessionId, AiApiUsage.RequestType.MODERATION, 
                          deepSeekConfig.getModel(), promptTokens[0], completionTokens[0], 
                          System.currentTimeMillis() - startTime, success, errorMessage, content);
            
            logger.error("Unexpected error during content moderation, using fallback: {}", e.getMessage(), e);
            return fallbackService.generateFallbackModerationResult(content, contentType);
        }
    }
    
    /**
     * Moderate content using DeepSeek API (backward compatibility)
     */
    public ModerationResult moderateContent(String content, String contentType) {
        return moderateContent(content, contentType, null, null);
    }
    
    /**
     * Build moderation request for DeepSeek API
     */
    private DeepSeekModerationRequest buildModerationRequest(String content) {
        DeepSeekModerationRequest.Message systemMessage = new DeepSeekModerationRequest.Message("system", MODERATION_SYSTEM_PROMPT);
        DeepSeekModerationRequest.Message userMessage = new DeepSeekModerationRequest.Message("user", 
            "Please moderate this content:\n\n" + content);
        
        return new DeepSeekModerationRequest(
            deepSeekConfig.getModel(),
            Arrays.asList(systemMessage, userMessage),
            0.1, // Low temperature for consistent decisions
            100, // Max tokens for short responses
            false
        );
    }
    
    /**
     * Call DeepSeek API with retry logic
     */
    private DeepSeekModerationResponse callDeepSeekAPI(DeepSeekModerationRequest request) {
        String url = deepSeekConfig.getBaseUrl() + "/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekConfig.getApiKey());
        
        HttpEntity<DeepSeekModerationRequest> entity = new HttpEntity<>(request, headers);
        
        for (int attempt = 1; attempt <= deepSeekConfig.getMaxRetries(); attempt++) {
            try {
                logger.debug("Calling DeepSeek API (attempt {}/{})", attempt, deepSeekConfig.getMaxRetries());
                
                ResponseEntity<DeepSeekModerationResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, DeepSeekModerationResponse.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    logger.debug("DeepSeek API call successful");
                    return response.getBody();
                } else {
                    throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
                }
                
            } catch (HttpClientErrorException e) {
                logger.error("Client error calling DeepSeek API (attempt {}): {}", attempt, e.getMessage());
                if (attempt == deepSeekConfig.getMaxRetries()) {
                    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                        throw AIServiceException.apiKeyError("Invalid API key: " + e.getMessage());
                    } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        throw AIServiceException.rateLimitError("Rate limit exceeded: " + e.getMessage());
                    } else if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
                        throw AIServiceException.quotaExceededError("Quota exceeded: " + e.getMessage());
                    } else {
                        throw AIServiceException.configurationError("Client error: " + e.getMessage());
                    }
                }
            } catch (HttpServerErrorException e) {
                logger.error("Server error calling DeepSeek API (attempt {}): {}", attempt, e.getMessage());
                if (attempt == deepSeekConfig.getMaxRetries()) {
                    throw AIServiceException.configurationError("Server error: " + e.getMessage());
                }
            } catch (ResourceAccessException e) {
                logger.error("Timeout calling DeepSeek API (attempt {}): {}", attempt, e.getMessage());
                if (attempt == deepSeekConfig.getMaxRetries()) {
                    throw AIServiceException.timeoutError("Request timeout: " + e.getMessage());
                }
            } catch (Exception e) {
                logger.error("Unexpected error calling DeepSeek API (attempt {}): {}", attempt, e.getMessage());
                if (attempt == deepSeekConfig.getMaxRetries()) {
                    throw AIServiceException.configurationError("Unexpected error: " + e.getMessage());
                }
            }
            
            // Wait before retry (exponential backoff)
            try {
                Thread.sleep(1000 * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during retry", ie);
            }
        }
        
        throw AIServiceException.configurationError("All retry attempts failed");
    }
    
    /**
     * Parse DeepSeek API response into ModerationResult
     */
    private ModerationResult parseModerationResponse(DeepSeekModerationResponse response, String originalContent) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            logger.warn("Empty response from DeepSeek API");
            return ModerationResult.safe("Empty API response");
        }
        
        String content = response.getChoices().get(0).getMessage().getContent();
        logger.debug("DeepSeek API response: {}", content);
        
        // Use the enhanced response parser
        ModerationResult result = ModerationResponseParser.parseResponse(content, originalContent);
        
        // Add usage information
        result.setTokensUsed(response.getUsage() != null ? response.getUsage().getTotalTokens() : 0);
        result.setCost(calculateCost(response.getUsage()));
        
        return result;
    }
    
    
    /**
     * Calculate cost based on token usage
     * DeepSeek pricing: $0.14 per 1M input tokens, $0.28 per 1M output tokens
     */
    private double calculateCost(DeepSeekModerationResponse.Usage usage) {
        if (usage == null) return 0.0;
        
        double inputCost = (usage.getPromptTokens() / 1_000_000.0) * 0.14;
        double outputCost = (usage.getCompletionTokens() / 1_000_000.0) * 0.28;
        
        return inputCost + outputCost;
    }
    
    
    /**
     * Check if the service is properly configured
     */
    public boolean isConfigured() {
        return deepSeekConfig.isValid();
    }
    
    /**
     * Generate chat completion using DeepSeek API
     * 
     * @param request The chat completion request
     * @param userId User ID for usage tracking (optional)
     * @param sessionId Session ID for usage tracking (optional)
     * @return DeepSeekModerationResponse with the AI-generated response
     */
    public DeepSeekModerationResponse generateChatCompletion(DeepSeekModerationRequest request, UUID userId, String sessionId) {
        // Validate configuration
        if (!isConfigured()) {
            throw AIServiceException.configurationError("DeepSeek API is not properly configured");
        }
        
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        int promptTokens = 0;
        int completionTokens = 0;
        
        try {
            logger.debug("Generating chat completion with model: {}", request.getModel());
            
            // Use circuit breaker with retry logic
            DeepSeekModerationResponse response = circuitBreaker.execute(() -> {
                return RetryUtil.executeWithRetry(
                    () -> callDeepSeekAPIWithRetry(request),
                    MAX_RETRY_ATTEMPTS,
                    createAIServiceRetryPredicate(),
                    INITIAL_RETRY_DELAY,
                    RETRY_BACKOFF_MULTIPLIER,
                    MAX_RETRY_DELAY,
                    JITTER_FACTOR
                );
            });
            
            // Extract usage information
            if (response.getUsage() != null) {
                promptTokens = response.getUsage().getPromptTokens();
                completionTokens = response.getUsage().getCompletionTokens();
            }
            
            // Record successful usage
            recordApiUsage(userId, sessionId, AiApiUsage.RequestType.CONTENT_GENERATION, 
                          request.getModel(), promptTokens, completionTokens, 
                          System.currentTimeMillis() - startTime, success, errorMessage, null);
            
            return response;
            
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            logger.warn("Circuit breaker is open for chat completion: {}", e.getMessage());
            throw AIServiceException.configurationError("AI service unavailable: " + e.getMessage());
        } catch (RetryUtil.RetryExhaustedException e) {
            logger.error("All retry attempts exhausted for chat completion: {}", e.getMessage());
            throw AIServiceException.configurationError("Chat completion failed after retries: " + e.getMessage());
        } catch (AIServiceException e) {
            success = false;
            errorMessage = e.getMessage();
            
            // Record failed usage
            recordApiUsage(userId, sessionId, AiApiUsage.RequestType.CONTENT_GENERATION, 
                          request.getModel(), promptTokens, completionTokens, 
                          System.currentTimeMillis() - startTime, success, errorMessage, null);
            
            // Re-throw AI service exceptions
            throw e;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            
            // Record failed usage
            recordApiUsage(userId, sessionId, AiApiUsage.RequestType.CONTENT_GENERATION, 
                          request.getModel(), promptTokens, completionTokens, 
                          System.currentTimeMillis() - startTime, success, errorMessage, null);
            
            logger.error("Error generating chat completion: {}", e.getMessage(), e);
            throw AIServiceException.configurationError("Chat completion failed: " + e.getMessage());
        }
    }
    
    /**
     * Generate chat completion using DeepSeek API (backward compatibility)
     */
    public DeepSeekModerationResponse generateChatCompletion(DeepSeekModerationRequest request) {
        return generateChatCompletion(request, null, null);
    }
    
    /**
     * Record API usage for tracking and billing
     */
    private void recordApiUsage(UUID userId, String sessionId, AiApiUsage.RequestType requestType, 
                              String modelName, int promptTokens, int completionTokens, 
                              long durationMs, boolean success, String errorMessage, String content) {
        try {
            BigDecimal costPerToken = costCalculationService.getCostPerToken(modelName);
            
            ApiUsageRequest usageRequest = ApiUsageRequest.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .requestType(requestType)
                    .modelName(modelName)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .costPerToken(costPerToken)
                    .requestDurationMs((int) durationMs)
                    .success(success)
                    .errorMessage(errorMessage)
                    .requestPayloadSize(content != null ? content.getBytes().length : 0)
                    .build();
            
            usageMonitoringService.recordUsage(usageRequest);
            
            logger.debug("Recorded API usage: {} - {} tokens, {}ms, ${}", 
                    requestType, promptTokens + completionTokens, durationMs, 
                    costPerToken.multiply(new BigDecimal(promptTokens + completionTokens)));
            
        } catch (Exception e) {
            logger.error("Failed to record API usage: {}", e.getMessage(), e);
            // Don't throw exception to avoid breaking the main flow
        }
    }
    
    /**
     * Call DeepSeek API with retry logic
     */
    private DeepSeekModerationResponse callDeepSeekAPIWithRetry(DeepSeekModerationRequest request) {
        try {
            return RetryUtil.executeWithRetry(
                () -> callDeepSeekAPI(request),
                MAX_RETRY_ATTEMPTS,
                createAIServiceRetryPredicate(),
                INITIAL_RETRY_DELAY,
                RETRY_BACKOFF_MULTIPLIER,
                MAX_RETRY_DELAY,
                JITTER_FACTOR
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call DeepSeek API with retry", e);
        }
    }
    
    /**
     * Create retry predicate for AI service calls
     */
    private Predicate<Exception> createAIServiceRetryPredicate() {
        return e -> {
            // Retry for network issues, timeouts, and temporary service errors
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            
            return message.contains("timeout") ||
                   message.contains("connection") ||
                   message.contains("network") ||
                   message.contains("temporary") ||
                   message.contains("service unavailable") ||
                   message.contains("rate limit") ||
                   e instanceof java.net.ConnectException ||
                   e instanceof java.net.SocketTimeoutException ||
                   e instanceof java.util.concurrent.TimeoutException ||
                   e instanceof ResourceAccessException ||
                   (e instanceof HttpServerErrorException && 
                    ((HttpServerErrorException) e).getStatusCode().is5xxServerError());
        };
    }
    
    /**
     * Get circuit breaker health status
     */
    public CircuitBreaker.CircuitBreakerHealth getCircuitBreakerHealth() {
        return circuitBreaker.getHealth();
    }
    
    /**
     * Reset circuit breaker
     */
    public void resetCircuitBreaker() {
        circuitBreaker.reset();
    }
    
    /**
     * Get service status for health checks
     */
    public String getServiceStatus() {
        if (!isConfigured()) {
            return "NOT_CONFIGURED";
        }
        if (!moderationConfig.isEnabled()) {
            return "DISABLED";
        }
        
        CircuitBreaker.State circuitState = circuitBreaker.getState();
        return switch (circuitState) {
            case CLOSED -> "READY";
            case OPEN -> "CIRCUIT_OPEN";
            case HALF_OPEN -> "RECOVERING";
        };
    }
    
    /**
     * Get detailed service health information
     */
    public Map<String, Object> getDetailedHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("configured", isConfigured());
        health.put("moderationEnabled", moderationConfig.isEnabled());
        health.put("serviceStatus", getServiceStatus());
        health.put("circuitBreakerHealth", getCircuitBreakerHealth());
        health.put("fallbackServiceStatus", fallbackService.getFallbackServiceStatus());
        health.put("timestamp", LocalDateTime.now());
        
        return health;
    }
    
    /**
     * Generate AI completion for various analysis tasks
     */
    public String generateCompletion(String prompt, String taskType) {
        try {
            logger.info("Generating AI completion for task: {}", taskType);
            
            // Create completion request
            DeepSeekModerationRequest request = new DeepSeekModerationRequest();
            request.setModel(deepSeekConfig.getModel());
            List<DeepSeekModerationRequest.Message> messages = Arrays.asList(
                new DeepSeekModerationRequest.Message("system", "You are a helpful AI assistant for a mental health support platform."),
                new DeepSeekModerationRequest.Message("user", prompt)
            );
            request.setMessages(messages);
            request.setMaxTokens(500);
            request.setTemperature(0.7);
            
            // Call AI service
            DeepSeekModerationResponse response = generateChatCompletion(request);
            
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String completion = response.getChoices().get(0).getMessage().getContent();
                logger.info("AI completion generated successfully for task: {}", taskType);
                return completion;
            } else {
                logger.warn("No completion generated for task: {}", taskType);
                return generateFallbackResponse(taskType);
            }
            
        } catch (Exception e) {
            logger.error("Error generating AI completion for task {}: {}", taskType, e.getMessage(), e);
            return generateFallbackResponse(taskType);
        }
    }
    
    /**
     * Generate fallback response when AI service is unavailable
     */
    private String generateFallbackResponse(String taskType) {
        switch (taskType) {
            case "sentiment_analysis":
                return "0.0"; // Neutral sentiment
            case "empathy_analysis":
                return "0.5"; // Moderate empathy
            case "question_generation":
                return "- How are you feeling about this situation?\n- What would be most helpful for you right now?\n- Would you like to talk more about this?";
            case "suggestions":
                return "- How can I help you today?\n- What's on your mind?\n- Would you like to talk about something specific?";
            case "analysis":
                return "Basic analysis completed. AI service temporarily unavailable.";
            default:
                return "AI service temporarily unavailable. Please try again later.";
        }
    }
}
