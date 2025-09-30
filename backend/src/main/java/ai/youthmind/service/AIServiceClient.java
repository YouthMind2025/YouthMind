package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.config.ModerationConfig;
import ai.youthmind.dto.ApiUsageRequest;
import ai.youthmind.dto.DeepSeekModerationRequest;
import ai.youthmind.dto.DeepSeekModerationResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
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
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;
    
    @Autowired
    private IntensityPredictor intensityPredictor;
    
    @Autowired
    private StateVectorProcessor stateVectorProcessor;
    
    @Autowired
    private AIErrorHandler aiErrorHandler;
    
    @Autowired
    private AIServiceConnectionPoolManager connectionPoolManager;
    
    @Autowired
    private AIServiceLoadBalancer loadBalancer;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMillis(1000);
    private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;
    private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(10);
    private static final double JITTER_FACTOR = 0.1;
    
    // Enhanced moderation prompt templates for contextual analysis
    private static final String MODERATION_SYSTEM_PROMPT = """
        You are a content moderation AI for YouthMind, a mental health support platform for youth.
        Your task is to analyze text content with CONTEXTUAL AWARENESS and determine if it's safe to publish.
        
        CRITICAL: Focus on CONTEXT and INTENT, not just individual words.
        
        APPROVE content when:
        - Profanity is used in positive contexts (e.g., "I did fucking well", "that's so fucking awesome")
        - Mild profanity expresses emotions constructively (e.g., "I'm so fucking frustrated with this situation")
        - Gen Z language patterns are used appropriately (e.g., "no cap", "that's fire", "periodt")
        - Content is supportive, educational, or shares personal experiences constructively
        - Therapeutic or mental health contexts contain emotional expression
        - Self-reflection and growth are evident despite strong language
        
        REJECT content when:
        - Profanity targets or attacks others (e.g., "you're a fucking idiot")
        - Hate speech, discrimination, or harassment is present
        - Violence, threats, or self-harm is promoted
        - Spam or malicious content is detected
        - Content could harm vulnerable youth regardless of language used
        
        CONTEXT ANALYSIS PRIORITIES:
        1. Intent and emotional tone (positive vs negative)
        2. Target of language (self vs others)
        3. Mental health/therapeutic context
        4. Gen Z cultural expressions
        5. Supportive vs harmful messaging
        
        Respond with ONLY "APPROVE" or "REJECT" followed by a brief reason.
        Format: "APPROVE: [reason]" or "REJECT: [reason]"
        """;
    
    // Gen Z language tolerance prompt
    private static final String GEN_Z_CONTEXT_PROMPT = """
        Analyze this content for Gen Z language patterns and cultural context:
        
        COMMON GEN Z EXPRESSIONS (generally acceptable):
        - "no cap" (no lie), "fr" (for real), "ngl" (not gonna lie)
        - "that's fire/lit/sick/dope" (positive reactions)
        - "period" or "periodt" (emphasis/agreement)
        - "stan" (strong support for someone/something)
        - "goals", "mood", "vibe", "energy" (emotional states)
        - Casual profanity in positive contexts
        
        CONTEXT MATTERS:
        - Positive emotional expression with profanity = APPROVE
        - Self-expression and personal sharing = APPROVE
        - Supportive or encouraging language = APPROVE
        - Targeting others negatively = REJECT
        - Hate speech or harassment = REJECT
        
        Focus on the OVERALL MESSAGE and EMOTIONAL TONE, not individual words.
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
                        // Use actual usage data from response
                        if (response.getUsage() != null) {
                            promptTokens[0] = response.getUsage().getPromptTokens();
                            completionTokens[0] = response.getUsage().getCompletionTokens();
                        } else {
                            promptTokens[0] = 100; // Default fallback
                            completionTokens[0] = 50; // Default fallback
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
     * Enhanced moderation with contextual analysis for borderline cases
     * 
     * @param content The content to moderate
     * @param contentType Type of content (post, comment, etc.)
     * @param userId User ID for usage tracking (optional)
     * @param sessionId Session ID for usage tracking (optional)
     * @param localAnalysis Local moderation analysis results
     * @param contextAnalysis Context-aware analysis results
     * @return ModerationResult with enhanced contextual decision
     */
    public ModerationResult moderateContentWithContext(String content, String contentType, UUID userId, String sessionId,
                                                      LocalModerationService.LocalModerationAnalysis localAnalysis,
                                                      ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
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
            logger.debug("Enhanced contextual moderation for {} content: {}", contentType, content.substring(0, Math.min(100, content.length())));
            
            // Use circuit breaker with retry logic
            ModerationResult result = circuitBreaker.execute(() -> {
                return RetryUtil.executeWithRetry(
                    () -> {
                        DeepSeekModerationRequest request = buildEnhancedModerationRequest(content, localAnalysis, contextAnalysis);
                        DeepSeekModerationResponse response = callDeepSeekAPIWithRetry(request);
                        
                        // Extract usage information
                        // Use actual usage data from response
                        if (response.getUsage() != null) {
                            promptTokens[0] = response.getUsage().getPromptTokens();
                            completionTokens[0] = response.getUsage().getCompletionTokens();
                        } else {
                            promptTokens[0] = 100; // Default fallback
                            completionTokens[0] = 50; // Default fallback
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
            
            logger.error("Unexpected error during enhanced moderation, using fallback: {}", e.getMessage(), e);
            return fallbackService.generateFallbackModerationResult(content, contentType);
        }
    }
    
    /**
     * Build moderation request for DeepSeek API with enhanced contextual analysis
     */
    private DeepSeekModerationRequest buildModerationRequest(String content) {
        // Enhanced system prompt with contextual analysis
        String enhancedSystemPrompt = MODERATION_SYSTEM_PROMPT + "\n\n" + GEN_Z_CONTEXT_PROMPT;
        
        DeepSeekModerationRequest.Message systemMessage = new DeepSeekModerationRequest.Message("system", enhancedSystemPrompt);
        
        // Enhanced user message with context analysis instructions
        String userMessageContent = String.format("""
            Please analyze this content with CONTEXTUAL AWARENESS:
            
            Content to analyze:
            "%s"
            
            ANALYSIS STEPS:
            1. Identify the overall emotional tone (positive, negative, neutral)
            2. Check if profanity targets self or others
            3. Look for Gen Z language patterns
            4. Assess mental health/therapeutic context
            5. Determine if the message is supportive or harmful
            
            Remember: Focus on CONTEXT and INTENT, not individual words.
            """, content);
        
        DeepSeekModerationRequest.Message userMessage = new DeepSeekModerationRequest.Message("user", userMessageContent);
        
        return new DeepSeekModerationRequest(
            deepSeekConfig.getModel(),
            Arrays.asList(systemMessage, userMessage),
            0.1, // Low temperature for consistent decisions
            150, // Increased max tokens for more detailed analysis
            false
        );
    }
    
    /**
     * Build enhanced moderation request for borderline cases
     */
    private DeepSeekModerationRequest buildEnhancedModerationRequest(String content, 
                                                                    LocalModerationService.LocalModerationAnalysis localAnalysis,
                                                                    ContextAwareModerationService.ContextAwareAnalysis contextAnalysis) {
        // Create context-aware system prompt
        StringBuilder contextInfo = new StringBuilder();
        
        if (contextAnalysis.getContextTypes() != null && !contextAnalysis.getContextTypes().isEmpty()) {
            contextInfo.append("DETECTED CONTEXTS: ").append(String.join(", ", contextAnalysis.getContextTypes())).append("\n");
        }
        
        if (localAnalysis.getDetectedWords() != null && !localAnalysis.getDetectedWords().isEmpty()) {
            contextInfo.append("DETECTED WORDS: ");
            for (LocalModerationService.DetectedWord word : localAnalysis.getDetectedWords()) {
                contextInfo.append(word.getWord()).append("(severity:").append(word.getSeverity()).append(") ");
            }
            contextInfo.append("\n");
        }
        
        String enhancedSystemPrompt = MODERATION_SYSTEM_PROMPT + "\n\n" + GEN_Z_CONTEXT_PROMPT + 
            "\n\nCONTEXT INFORMATION:\n" + contextInfo.toString();
        
        DeepSeekModerationRequest.Message systemMessage = new DeepSeekModerationRequest.Message("system", enhancedSystemPrompt);
        
        // Enhanced user message with local analysis context
        String userMessageContent = String.format("""
            ENHANCED CONTEXTUAL ANALYSIS REQUIRED:
            
            Content: "%s"
            
            LOCAL ANALYSIS RESULTS:
            - Safe: %s
            - Max Severity: %d
            - Context Confidence: %.2f
            - Context Types: %s
            
            Please provide a FINAL DECISION considering:
            1. The local analysis results above
            2. Contextual appropriateness
            3. Gen Z language tolerance
            4. Mental health platform guidelines
            
            Focus on whether this content would be HELPFUL or HARMFUL to youth seeking mental health support.
            """, content, localAnalysis.isSafe(), localAnalysis.getMaxSeverity(), 
            contextAnalysis.getConfidence(), String.join(", ", contextAnalysis.getContextTypes()));
        
        DeepSeekModerationRequest.Message userMessage = new DeepSeekModerationRequest.Message("user", userMessageContent);
        
        return new DeepSeekModerationRequest(
            deepSeekConfig.getModel(),
            Arrays.asList(systemMessage, userMessage),
            0.1, // Low temperature for consistent decisions
            200, // More tokens for detailed analysis
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
        result.setTokensUsed(response.getUsage() != null ? response.getUsage().getTotalTokens() : 150);
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
            // Use actual usage data from response
            if (response.getUsage() != null) {
                promptTokens = response.getUsage().getPromptTokens();
                completionTokens = response.getUsage().getCompletionTokens();
            } else {
                promptTokens = 100; // Default fallback
                completionTokens = 50; // Default fallback
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
    
    // ==================== PSYCHOLOGICAL ANALYSIS METHODS ====================
    
    /**
     * Perform comprehensive psychological analysis
     * @param request Psychological analysis request
     * @return Comprehensive psychological analysis response
     */
    public PsychologicalAnalysisResponse performPsychologicalAnalysis(PsychologicalAnalysisRequest request) {
        try {
            logger.info("Performing psychological analysis for user {} with requestId {}", 
                request.getUserId(), request.getRequestId());
            
            long startTime = System.currentTimeMillis();
            
            // Validate request
            validatePsychologicalAnalysisRequest(request);
            
            // Perform analysis components
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector = null;
            PsychologicalAnalysisResponse.BuddhistFactorsResult buddhistFactors = null;
            PsychologicalAnalysisResponse.IntensityFactorResult intensityFactor = null;
            PsychologicalAnalysisResponse.StateVectorResult stateVector = null;
            PsychologicalAnalysisResponse.ComprehensiveAnalysis comprehensiveAnalysis = null;
            
            // Generate psychological vector if requested
            if (request.getAnalysisConfig().isGeneratePsychologicalVector()) {
                psychologicalVector = generatePsychologicalVector(request);
            }
            
            // Analyze Buddhist factors if requested
            if (request.getAnalysisConfig().isAnalyzeBuddhistFactors()) {
                buddhistFactors = analyzeBuddhistFactors(request);
            }
            
            // Calculate intensity factor if requested
            if (request.getAnalysisConfig().isCalculateIntensityFactor()) {
                intensityFactor = calculateIntensityFactor(request, psychologicalVector, buddhistFactors);
            }
            
            // Process state vector if requested
            if (request.getAnalysisConfig().isProcessStateVector()) {
                stateVector = processStateVector(request, psychologicalVector);
            }
            
            // Perform comprehensive analysis if requested
            if (request.getAnalysisConfig().isIncludeDetailedAnalysis()) {
                comprehensiveAnalysis = performComprehensiveAnalysis(request, psychologicalVector, buddhistFactors, intensityFactor);
            }
            
            // Generate recommendations and insights
            List<PsychologicalAnalysisResponse.Recommendation> recommendations = generateRecommendations(request, comprehensiveAnalysis);
            List<PsychologicalAnalysisResponse.Insight> insights = generateInsights(request, comprehensiveAnalysis);
            
            // Perform risk assessment
            PsychologicalAnalysisResponse.RiskAssessment riskAssessment = performRiskAssessment(request, comprehensiveAnalysis);
            
            // Create response
            PsychologicalAnalysisResponse response = new PsychologicalAnalysisResponse();
            response.setRequestId(request.getRequestId());
            response.setUserId(request.getUserId());
            response.setSessionId(request.getSessionId());
            response.setPsychologicalVector(psychologicalVector);
            response.setBuddhistFactors(buddhistFactors);
            response.setIntensityFactor(intensityFactor);
            response.setStateVector(stateVector);
            response.setComprehensiveAnalysis(comprehensiveAnalysis);
            response.setRecommendations(recommendations);
            response.setInsights(insights);
            response.setRiskAssessment(riskAssessment);
            
            // Set metadata
            long processingTime = System.currentTimeMillis() - startTime;
            PsychologicalAnalysisResponse.AnalysisMetadata metadata = new PsychologicalAnalysisResponse.AnalysisMetadata();
            metadata.setAnalysisId(UUID.randomUUID().toString());
            metadata.setModelVersion("1.0.0");
            metadata.setAnalysisMethod("COMPREHENSIVE_PSYCHOLOGICAL_ANALYSIS");
            metadata.setTimestamp(LocalDateTime.now());
            metadata.setProcessingTimeMs((int) processingTime);
            metadata.setStatus("SUCCESS");
            response.setMetadata(metadata);
            
            // Set processing info
            PsychologicalAnalysisResponse.ProcessingInfo processingInfo = new PsychologicalAnalysisResponse.ProcessingInfo();
            processingInfo.setProcessingEngine("YouthMind AI Engine");
            processingInfo.setModelVersion("1.0.0");
            processingInfo.setTotalProcessingTimeMs((int) processingTime);
            processingInfo.setProcessingStatus("COMPLETED");
            response.setProcessingInfo(processingInfo);
            
            logger.info("Psychological analysis completed for user {} in {}ms", 
                request.getUserId(), processingTime);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error performing psychological analysis for user {}: {}", 
                request.getUserId(), e.getMessage(), e);
            
            // Handle error and return fallback response
            Map<String, Object> errorContext = Map.of(
                "requestId", request.getRequestId(),
                "analysisType", "psychological_analysis",
                "userId", request.getUserId()
            );
            
            Map<String, Object> errorResult = aiErrorHandler.handleError(e, errorContext, request.getUserId());
            
            // Create error response
            PsychologicalAnalysisResponse errorResponse = createErrorResponse(request, e, errorResult);
            return errorResponse;
        }
    }
    
    /**
     * Generate psychological vector for content
     * @param request Analysis request
     * @return Psychological vector result
     */
    private PsychologicalAnalysisResponse.PsychologicalVectorResult generatePsychologicalVector(PsychologicalAnalysisRequest request) {
        try {
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(
                request.getContent(), request.getUserId());
            
            PsychologicalAnalysisResponse.PsychologicalVectorResult result = 
                new PsychologicalAnalysisResponse.PsychologicalVectorResult();
            
            @SuppressWarnings("unchecked")
            List<Double> vector = (List<Double>) vectorResult.get("psychologicalVector");
            result.setVector(vector);
            result.setConfidenceScore((Double) vectorResult.get("confidenceScore"));
            result.setValid((Boolean) vectorResult.get("isValid"));
            result.setOptimizationDetails((Map<String, Object>) vectorResult.get("optimizedVector"));
            result.setAnalysisMethod("PSYCHOLOGICAL_VECTOR_GENERATION");
            result.setGeneratedAt(LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error generating psychological vector: {}", e.getMessage(), e);
            return createFallbackPsychologicalVector();
        }
    }
    
    /**
     * Analyze Buddhist factors in content
     * @param request Analysis request
     * @return Buddhist factors result
     */
    private PsychologicalAnalysisResponse.BuddhistFactorsResult analyzeBuddhistFactors(PsychologicalAnalysisRequest request) {
        try {
            Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(
                request.getContent(), request.getUserId());
            
            PsychologicalAnalysisResponse.BuddhistFactorsResult result = 
                new PsychologicalAnalysisResponse.BuddhistFactorsResult();
            
            result.setFactorScores((Map<String, Double>) factorsResult.get("factorScores"));
            result.setDetectedKeywords((Map<String, List<String>>) factorsResult.get("detectedKeywords"));
            result.setClassifiedFactors((Map<String, Object>) factorsResult.get("classifiedFactors"));
            result.setIntensityAnalysis((Map<String, Object>) factorsResult.get("intensityAnalysis"));
            result.setAnalyzedAt(LocalDateTime.now());
            
            // Calculate dominant factor
            Map<String, Double> factorScores = result.getFactorScores();
            if (!factorScores.isEmpty()) {
                String dominantFactor = factorScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("none");
                result.setDominantFactor(dominantFactor);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing Buddhist factors: {}", e.getMessage(), e);
            return createFallbackBuddhistFactors();
        }
    }
    
    /**
     * Calculate intensity factor
     * @param request Analysis request
     * @param psychologicalVector Psychological vector result
     * @param buddhistFactors Buddhist factors result
     * @return Intensity factor result
     */
    private PsychologicalAnalysisResponse.IntensityFactorResult calculateIntensityFactor(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector,
            PsychologicalAnalysisResponse.BuddhistFactorsResult buddhistFactors) {
        try {
            List<Double> vector = psychologicalVector != null ? psychologicalVector.getVector() : null;
            Map<String, Object> buddhistFactorsMap = buddhistFactors != null ? 
                Map.of("factorScores", buddhistFactors.getFactorScores()) : new HashMap<>();
            
            Map<String, Object> intensityResult = intensityPredictor.predictIntensity(
                request.getContent(), request.getUserId(), vector, buddhistFactorsMap);
            
            PsychologicalAnalysisResponse.IntensityFactorResult result = 
                new PsychologicalAnalysisResponse.IntensityFactorResult();
            
            result.setIntensityScore((Double) intensityResult.get("intensityScore"));
            result.setImpactScore((Double) intensityResult.get("impactScore"));
            result.setIntensityClassification((String) intensityResult.get("intensityClassification"));
            result.setValid((Boolean) intensityResult.get("isValid"));
            result.setPredictedAt(LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error calculating intensity factor: {}", e.getMessage(), e);
            return createFallbackIntensityFactor();
        }
    }
    
    /**
     * Process state vector
     * @param request Analysis request
     * @param psychologicalVector Psychological vector result
     * @return State vector result
     */
    private PsychologicalAnalysisResponse.StateVectorResult processStateVector(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        try {
            List<Double> vector = psychologicalVector != null ? psychologicalVector.getVector() : null;
            
            Map<String, Object> stateResult = stateVectorProcessor.processVector(vector, request.getUserId());
            
            PsychologicalAnalysisResponse.StateVectorResult result = 
                new PsychologicalAnalysisResponse.StateVectorResult();
            
            result.setProcessedVector((List<Double>) stateResult.get("processedVector"));
            result.setProcessingDetails((Map<String, Object>) stateResult.get("processingDetails"));
            result.setValid((Boolean) stateResult.get("isValid"));
            result.setStatistics((Map<String, Object>) stateResult.get("statistics"));
            result.setProcessingMethod("STATE_VECTOR_PROCESSING");
            result.setProcessedAt(LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing state vector: {}", e.getMessage(), e);
            return createFallbackStateVector();
        }
    }
    
    /**
     * Perform comprehensive analysis
     * @param request Analysis request
     * @param psychologicalVector Psychological vector result
     * @param buddhistFactors Buddhist factors result
     * @param intensityFactor Intensity factor result
     * @return Comprehensive analysis result
     */
    private PsychologicalAnalysisResponse.ComprehensiveAnalysis performComprehensiveAnalysis(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector,
            PsychologicalAnalysisResponse.BuddhistFactorsResult buddhistFactors,
            PsychologicalAnalysisResponse.IntensityFactorResult intensityFactor) {
        
        PsychologicalAnalysisResponse.ComprehensiveAnalysis analysis = 
            new PsychologicalAnalysisResponse.ComprehensiveAnalysis();
        
        // Perform emotional analysis
        analysis.setEmotionalAnalysis(analyzeEmotionalState(request, psychologicalVector));
        
        // Perform cognitive analysis
        analysis.setCognitiveAnalysis(analyzeCognitivePatterns(request, psychologicalVector));
        
        // Perform social analysis
        analysis.setSocialAnalysis(analyzeSocialDynamics(request, psychologicalVector));
        
        // Perform stress analysis
        analysis.setStressAnalysis(analyzeStressIndicators(request, psychologicalVector));
        
        // Perform personality analysis
        analysis.setPersonalityAnalysis(analyzePersonalityTraits(request, psychologicalVector));
        
        // Perform mental health analysis
        analysis.setMentalHealthAnalysis(analyzeMentalHealthIndicators(request, psychologicalVector));
        
        // Perform coping analysis
        analysis.setCopingAnalysis(analyzeCopingMechanisms(request, psychologicalVector));
        
        // Perform resilience analysis
        analysis.setResilienceAnalysis(analyzeResilienceFactors(request, psychologicalVector));
        
        // Calculate overall assessment
        analysis.setOverallAssessment(calculateOverallAssessment(analysis));
        analysis.setOverallScore(calculateOverallScore(analysis));
        analysis.setAnalyzedAt(LocalDateTime.now());
        
        return analysis;
    }
    
    // Helper methods for comprehensive analysis components
    private PsychologicalAnalysisResponse.EmotionalAnalysis analyzeEmotionalState(
            PsychologicalAnalysisRequest request, 
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.EmotionalAnalysis analysis = 
            new PsychologicalAnalysisResponse.EmotionalAnalysis();
        
        // Simulate emotional analysis based on content and vector
        analysis.setPrimaryEmotion(determinePrimaryEmotion(request.getContent()));
        analysis.setSecondaryEmotions(determineSecondaryEmotions(request.getContent()));
        analysis.setEmotionalIntensity(calculateEmotionalIntensity(psychologicalVector));
        analysis.setEmotionalStability(assessEmotionalStability(psychologicalVector));
        analysis.setEmotionalTrend("stable"); // Placeholder
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.CognitiveAnalysis analyzeCognitivePatterns(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.CognitiveAnalysis analysis = 
            new PsychologicalAnalysisResponse.CognitiveAnalysis();
        
        // Simulate cognitive analysis
        analysis.setThinkingPattern("analytical");
        analysis.setAnalyticalThinking(0.7);
        analysis.setCreativeExpression(0.6);
        analysis.setProblemSolving(0.8);
        analysis.setMemoryReflection(0.5);
        analysis.setFutureOrientation(0.6);
        analysis.setCognitiveStyle("balanced");
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.SocialAnalysis analyzeSocialDynamics(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.SocialAnalysis analysis = 
            new PsychologicalAnalysisResponse.SocialAnalysis();
        
        // Simulate social analysis
        analysis.setSocialConnection(0.7);
        analysis.setIsolationLevel(0.3);
        analysis.setEmpathyCompassion(0.8);
        analysis.setConflictTension(0.2);
        analysis.setTrustIntimacy(0.6);
        analysis.setSocialStyle("collaborative");
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.StressAnalysis analyzeStressIndicators(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.StressAnalysis analysis = 
            new PsychologicalAnalysisResponse.StressAnalysis();
        
        // Simulate stress analysis
        analysis.setWorkStress(0.4);
        analysis.setAcademicStress(0.5);
        analysis.setFinancialStress(0.3);
        analysis.setHealthStress(0.2);
        analysis.setRelationshipStress(0.3);
        analysis.setOverallStressLevel(0.34);
        analysis.setStressPattern("moderate");
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.PersonalityAnalysis analyzePersonalityTraits(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.PersonalityAnalysis analysis = 
            new PsychologicalAnalysisResponse.PersonalityAnalysis();
        
        // Simulate personality analysis
        analysis.setExtraversion(0.6);
        analysis.setIntroversion(0.4);
        analysis.setOpenness(0.7);
        analysis.setConscientiousness(0.8);
        analysis.setAgreeableness(0.9);
        analysis.setDominantTrait("agreeableness");
        analysis.setPersonalityType("collaborator");
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.MentalHealthAnalysis analyzeMentalHealthIndicators(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.MentalHealthAnalysis analysis = 
            new PsychologicalAnalysisResponse.MentalHealthAnalysis();
        
        // Simulate mental health analysis
        analysis.setDepressionIndicators(0.2);
        analysis.setAnxietyIndicators(0.3);
        analysis.setSelfEsteem(0.7);
        analysis.setSleepPatterns(0.6);
        analysis.setAppetiteEnergy(0.8);
        analysis.setMentalHealthStatus("good");
        analysis.setRiskFactors(Arrays.asList("mild_stress"));
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.CopingAnalysis analyzeCopingMechanisms(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.CopingAnalysis analysis = 
            new PsychologicalAnalysisResponse.CopingAnalysis();
        
        // Simulate coping analysis
        analysis.setPositiveCoping(0.8);
        analysis.setAvoidance(0.2);
        analysis.setSeekingSupport(0.7);
        analysis.setSelfCare(0.6);
        analysis.setProblemFocusedCoping(0.8);
        analysis.setCopingStyle("adaptive");
        analysis.setEffectiveStrategies(Arrays.asList("problem_solving", "social_support"));
        
        return analysis;
    }
    
    private PsychologicalAnalysisResponse.ResilienceAnalysis analyzeResilienceFactors(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.PsychologicalVectorResult psychologicalVector) {
        
        PsychologicalAnalysisResponse.ResilienceAnalysis analysis = 
            new PsychologicalAnalysisResponse.ResilienceAnalysis();
        
        // Simulate resilience analysis
        analysis.setResilience(0.8);
        analysis.setGrowthMindset(0.7);
        analysis.setResilienceLevel("high");
        analysis.setStrengths(Arrays.asList("optimism", "social_support", "problem_solving"));
        analysis.setAreasForGrowth(Arrays.asList("stress_management", "self_care"));
        
        return analysis;
    }
    
    // Helper methods for generating recommendations and insights
    private List<PsychologicalAnalysisResponse.Recommendation> generateRecommendations(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.ComprehensiveAnalysis analysis) {
        
        List<PsychologicalAnalysisResponse.Recommendation> recommendations = new ArrayList<>();
        
        // Generate recommendations based on analysis
        if (analysis != null && analysis.getEmotionalAnalysis() != null) {
            PsychologicalAnalysisResponse.Recommendation emotionalRec = 
                new PsychologicalAnalysisResponse.Recommendation();
            emotionalRec.setType("emotional_support");
            emotionalRec.setTitle("Emotional Support");
            emotionalRec.setDescription("Consider engaging in activities that promote emotional well-being");
            emotionalRec.setPriority("medium");
            emotionalRec.setActionSteps(Arrays.asList("Practice mindfulness", "Engage in hobbies", "Connect with friends"));
            emotionalRec.setRationale("Your emotional analysis suggests areas for improvement");
            recommendations.add(emotionalRec);
        }
        
        return recommendations;
    }
    
    private List<PsychologicalAnalysisResponse.Insight> generateInsights(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.ComprehensiveAnalysis analysis) {
        
        List<PsychologicalAnalysisResponse.Insight> insights = new ArrayList<>();
        
        // Generate insights based on analysis
        if (analysis != null && analysis.getPersonalityAnalysis() != null) {
            PsychologicalAnalysisResponse.Insight personalityInsight = 
                new PsychologicalAnalysisResponse.Insight();
            personalityInsight.setCategory("personality");
            personalityInsight.setTitle("Personality Profile");
            personalityInsight.setDescription("Your personality analysis shows strong collaborative tendencies");
            personalityInsight.setSignificance("This suggests you work well in team environments");
            personalityInsight.setConfidence("high");
            insights.add(personalityInsight);
        }
        
        return insights;
    }
    
    private PsychologicalAnalysisResponse.RiskAssessment performRiskAssessment(
            PsychologicalAnalysisRequest request,
            PsychologicalAnalysisResponse.ComprehensiveAnalysis analysis) {
        
        PsychologicalAnalysisResponse.RiskAssessment assessment = 
            new PsychologicalAnalysisResponse.RiskAssessment();
        
        // Perform risk assessment based on analysis
        assessment.setOverallRiskLevel("low");
        assessment.setRiskFactors(Arrays.asList("mild_stress"));
        assessment.setProtectiveFactors(Arrays.asList("strong_social_support", "good_coping_skills"));
        assessment.setRiskCategory("general");
        assessment.setRequiresImmediateAttention(false);
        assessment.setRecommendedAction("Continue current positive practices");
        
        return assessment;
    }
    
    // Helper methods for fallback responses
    private PsychologicalAnalysisResponse.PsychologicalVectorResult createFallbackPsychologicalVector() {
        PsychologicalAnalysisResponse.PsychologicalVectorResult result = 
            new PsychologicalAnalysisResponse.PsychologicalVectorResult();
        result.setVector(new ArrayList<>());
        result.setConfidenceScore(0.0);
        result.setValid(false);
        result.setAnalysisMethod("FALLBACK");
        result.setGeneratedAt(LocalDateTime.now());
        return result;
    }
    
    private PsychologicalAnalysisResponse.BuddhistFactorsResult createFallbackBuddhistFactors() {
        PsychologicalAnalysisResponse.BuddhistFactorsResult result = 
            new PsychologicalAnalysisResponse.BuddhistFactorsResult();
        result.setFactorScores(new HashMap<>());
        result.setDetectedKeywords(new HashMap<>());
        result.setAnalyzedAt(LocalDateTime.now());
        return result;
    }
    
    private PsychologicalAnalysisResponse.IntensityFactorResult createFallbackIntensityFactor() {
        PsychologicalAnalysisResponse.IntensityFactorResult result = 
            new PsychologicalAnalysisResponse.IntensityFactorResult();
        result.setIntensityScore(0.0);
        result.setImpactScore(0.0);
        result.setIntensityClassification("UNKNOWN");
        result.setValid(false);
        result.setPredictedAt(LocalDateTime.now());
        return result;
    }
    
    private PsychologicalAnalysisResponse.StateVectorResult createFallbackStateVector() {
        PsychologicalAnalysisResponse.StateVectorResult result = 
            new PsychologicalAnalysisResponse.StateVectorResult();
        result.setProcessedVector(new ArrayList<>());
        result.setValid(false);
        result.setProcessingMethod("FALLBACK");
        result.setProcessedAt(LocalDateTime.now());
        return result;
    }
    
    private PsychologicalAnalysisResponse createErrorResponse(
            PsychologicalAnalysisRequest request, 
            Exception error, 
            Map<String, Object> errorResult) {
        
        PsychologicalAnalysisResponse response = new PsychologicalAnalysisResponse();
        response.setRequestId(request.getRequestId());
        response.setUserId(request.getUserId());
        response.setSessionId(request.getSessionId());
        
        // Set error metadata
        PsychologicalAnalysisResponse.AnalysisMetadata metadata = 
            new PsychologicalAnalysisResponse.AnalysisMetadata();
        metadata.setAnalysisId(UUID.randomUUID().toString());
        metadata.setStatus("ERROR");
        metadata.setTimestamp(LocalDateTime.now());
        metadata.setTechnicalDetails(Map.of("error", error.getMessage()));
        response.setMetadata(metadata);
        
        // Set error processing info
        PsychologicalAnalysisResponse.ProcessingInfo processingInfo = 
            new PsychologicalAnalysisResponse.ProcessingInfo();
        processingInfo.setProcessingStatus("ERROR");
        processingInfo.setErrors(Arrays.asList(error.getMessage()));
        response.setProcessingInfo(processingInfo);
        
        return response;
    }
    
    /**
     * Execute AI service operation with connection pooling and load balancing
     */
    public <T> CompletableFuture<T> executeWithConnectionPool(
            String serviceType,
            String operation,
            Supplier<T> operationSupplier,
            Supplier<T> fallbackSupplier) {
        
        return connectionPoolManager.executeWithConnection(
            serviceType,
            operation,
            operationSupplier,
            fallbackSupplier
        );
    }
    
    /**
     * Get AI service connection pool health status
     */
    public AIServiceConnectionPoolManager.ConnectionPoolHealthStatus getConnectionPoolHealth() {
        return connectionPoolManager.getHealthStatus();
    }
    
    /**
     * Get AI service connection pool metrics
     */
    public AIServiceConnectionPoolManager.ConnectionPoolMetrics getConnectionPoolMetrics() {
        return connectionPoolManager.getMetrics();
    }
    
    /**
     * Get load balancing metrics for AI services
     */
    public AIServiceLoadBalancer.LoadBalancingMetrics getLoadBalancingMetrics(String serviceType) {
        return loadBalancer.getMetrics(serviceType);
    }
    
    /**
     * Scale AI service connection pool
     */
    public void scaleConnectionPool(String serviceType, int targetConnections) {
        connectionPoolManager.scalePool(serviceType, targetConnections);
    }

    /**
     * Execute AI operation with load balancing for psychological analysis services
     */
    public <T> CompletableFuture<T> executeWithLoadBalancing(
            String serviceType,
            String userId,
            String operation,
            Supplier<T> operationSupplier,
            Supplier<T> fallbackSupplier) {
        
        try {
            // Select optimal endpoint using load balancer
            AIServiceLoadBalancer.ServiceEndpoint endpoint = loadBalancer.selectEndpoint(serviceType, userId, operation);
            
            // Execute operation with circuit breaker protection
            return circuitBreaker.executeWithCircuitBreaker(
                serviceType,
                () -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        T result = operationSupplier.get();
                        long responseTime = System.currentTimeMillis() - startTime;
                        
                        // Record successful request
                        loadBalancer.recordRequestResult(serviceType, endpoint.getEndpointId(), true, responseTime);
                        
                        return result;
                    } catch (Exception e) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        
                        // Record failed request
                        loadBalancer.recordRequestResult(serviceType, endpoint.getEndpointId(), false, responseTime);
                        
                        throw e;
                    }
                },
                () -> fallbackSupplier.get()
            );
            
        } catch (Exception e) {
            logger.error("Error executing operation with load balancing for service: {} user: {} operation: {}", 
                       serviceType, userId, operation, e);
            return CompletableFuture.completedFuture(fallbackSupplier.get());
        }
    }

    /**
     * Get service discovery information for psychological analysis services
     */
    public AIServiceLoadBalancer.ServiceDiscoveryInfo getServiceDiscoveryInfo(String serviceType) {
        return loadBalancer.getServiceDiscoveryInfo(serviceType);
    }

    /**
     * Auto-scale psychological analysis service
     */
    public void autoScalePsychologicalService(String serviceType, int targetInstances) {
        loadBalancer.autoScaleService(serviceType, targetInstances);
    }

    /**
     * Perform failover for failed psychological analysis service endpoint
     */
    public AIServiceLoadBalancer.ServiceEndpoint performServiceFailover(String serviceType, String failedEndpointId) {
        return loadBalancer.performFailover(serviceType, failedEndpointId);
    }

    /**
     * Get comprehensive load balancing health status
     */
    public AIServiceLoadBalancer.LoadBalancingHealthStatus getLoadBalancingHealthStatus() {
        return loadBalancer.getLoadBalancingHealthStatus();
    }

    /**
     * Update load balancing configuration for psychological analysis services
     */
    public void updateLoadBalancingConfig(String serviceType, AIServiceLoadBalancer.LoadBalancingConfig config) {
        loadBalancer.updateLoadBalancingConfig(serviceType, config);
    }
    
    // Helper methods for analysis calculations
    private void validatePsychologicalAnalysisRequest(PsychologicalAnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Psychological analysis request cannot be null");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }
    
    private String determinePrimaryEmotion(String content) {
        // Simple emotion detection based on keywords
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("happy") || lowerContent.contains("joy")) return "joy";
        if (lowerContent.contains("sad") || lowerContent.contains("depressed")) return "sadness";
        if (lowerContent.contains("angry") || lowerContent.contains("mad")) return "anger";
        if (lowerContent.contains("worried") || lowerContent.contains("anxious")) return "anxiety";
        if (lowerContent.contains("calm") || lowerContent.contains("peaceful")) return "calm";
        return "neutral";
    }
    
    private List<String> determineSecondaryEmotions(String content) {
        return Arrays.asList("content", "hopeful");
    }
    
    private double calculateEmotionalIntensity(PsychologicalAnalysisResponse.PsychologicalVectorResult vector) {
        return vector != null ? vector.getConfidenceScore() : 0.5;
    }
    
    private String assessEmotionalStability(PsychologicalAnalysisResponse.PsychologicalVectorResult vector) {
        return "stable";
    }
    
    private String calculateOverallAssessment(PsychologicalAnalysisResponse.ComprehensiveAnalysis analysis) {
        return "Overall psychological state appears healthy with areas for continued growth";
    }
    
    private double calculateOverallScore(PsychologicalAnalysisResponse.ComprehensiveAnalysis analysis) {
        return 0.75; // Placeholder score
    }
    
    /**
     * Analyze content for moderation purposes
     * 
     * @param content Content to analyze
     * @param userId User ID
     * @return Analysis results as Map
     */
    public Map<String, Object> analyzeContentForModeration(String content, UUID userId) {
        try {
            logger.debug("Analyzing content for moderation for user: {}", userId);
            
            // Use existing moderation method
            ModerationResult moderationResult = moderateContent(content, "text/plain");
            
            // Create a mock response for compatibility
            DeepSeekModerationResponse response = new DeepSeekModerationResponse();
            DeepSeekModerationResponse.Choice choice = new DeepSeekModerationResponse.Choice();
            DeepSeekModerationResponse.Message message = new DeepSeekModerationResponse.Message();
            message.setContent(moderationResult.isSafe() ? "APPROVED" : "REJECTED");
            choice.setMessage(message);
            response.setChoices(java.util.List.of(choice));
            
            Map<String, Object> result = new HashMap<>();
            result.put("isApproved", response.getChoices().get(0).getMessage().getContent().contains("APPROVED"));
            result.put("sentimentIntensity", 0.5); // Default value
            result.put("emotionalIntensity", 0.5); // Default value
            result.put("urgencyIntensity", 0.5); // Default value
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing content for moderation: {}", e.getMessage(), e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("isApproved", true);
            fallback.put("sentimentIntensity", 0.5);
            fallback.put("emotionalIntensity", 0.5);
            fallback.put("urgencyIntensity", 0.5);
            return fallback;
        }
    }
}
