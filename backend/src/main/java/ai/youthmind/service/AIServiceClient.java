package ai.youthmind.service;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.config.ModerationConfig;
import ai.youthmind.dto.DeepSeekModerationRequest;
import ai.youthmind.dto.DeepSeekModerationResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.exception.AIServiceException;
import ai.youthmind.util.ModerationResponseParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
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
     * @return ModerationResult with decision and details
     */
    public ModerationResult moderateContent(String content, String contentType) {
        // Validate configuration
        if (!isConfigured()) {
            throw AIServiceException.configurationError("DeepSeek API is not properly configured");
        }
        
        if (!moderationConfig.isEnabled()) {
            logger.debug("Moderation is disabled, approving content by default");
            return ModerationResult.safe("Moderation disabled");
        }
        
        try {
            logger.debug("Moderating {} content: {}", contentType, content.substring(0, Math.min(100, content.length())));
            
            DeepSeekModerationRequest request = buildModerationRequest(content);
            DeepSeekModerationResponse response = callDeepSeekAPI(request);
            
            return parseModerationResponse(response, content);
            
        } catch (AIServiceException e) {
            // Re-throw AI service exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Error during content moderation: {}", e.getMessage(), e);
            throw AIServiceException.configurationError("Content moderation failed: " + e.getMessage());
        }
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
     * Get service status for health checks
     */
    public String getServiceStatus() {
        if (!isConfigured()) {
            return "NOT_CONFIGURED";
        }
        if (!moderationConfig.isEnabled()) {
            return "DISABLED";
        }
        return "READY";
    }
}
