package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Fallback service for AI operations when AI services are unavailable
 * Provides graceful degradation and basic functionality without AI
 */
@Service
public class AIFallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIFallbackService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private AIHealthCheckService healthCheckService;
    
    @Value("${ai.deepseek.base-url:}")
    private String deepSeekBaseUrl;
    
    @Value("${ai.deepseek.api-key:}")
    private String deepSeekApiKey;
    
    @Value("${ai.deepseek.enabled:true}")
    private boolean aiEnabled;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Fallback response templates
    private static final Map<String, String> FALLBACK_RESPONSES = Map.of(
        "supportive", "I'm here to support you through whatever you're facing. " +
                     "While our AI features are temporarily unavailable, I want you to know that " +
                     "your feelings are valid and important. Please consider reaching out to a " +
                     "trusted friend, family member, or mental health professional for support.",
        
        "professional", "I understand you're seeking guidance. While our AI analysis is currently " +
                       "unavailable, I recommend consulting with a qualified mental health professional " +
                       "who can provide the specialized support you need. In the meantime, consider " +
                       "practicing self-care and reaching out to your support network.",
        
        "casual", "Hey! I'm here for you, even though our AI buddy is taking a little break. " +
                 "Whatever you're going through, you're not alone. Maybe try talking to a friend " +
                 "or doing something that usually makes you feel better? I'm rooting for you!",
        
        "empathetic", "I can sense that you're going through something difficult right now. " +
                     "While I can't provide the full AI-powered support at the moment, I want you " +
                     "to know that your feelings matter and it's okay to not be okay. Please don't " +
                     "hesitate to reach out to someone you trust or a mental health professional."
    );
    
    // Crisis response templates
    private static final List<String> CRISIS_RESPONSES = Arrays.asList(
        "I'm concerned about what you're sharing. Please reach out to a mental health professional " +
        "or crisis hotline immediately. You can call the National Suicide Prevention Lifeline at " +
        "988 or text HOME to 741741 for crisis support.",
        
        "Your safety is important. If you're having thoughts of self-harm, please contact a " +
        "crisis counselor right away. The Crisis Text Line is available 24/7 by texting HOME to 741741.",
        
        "I want to make sure you're safe. Please reach out to a trusted adult, mental health " +
        "professional, or crisis hotline. You don't have to face this alone."
    );
    
    /**
     * Generate fallback AI chat response
     */
    public AIChatResponse generateFallbackChatResponse(AIChatRequest request, UUID userId) {
        logger.info("Generating fallback AI chat response for user: {}", userId);
        
        try {
            // Get user personality profile for appropriate response
            String personalityType = getUserPersonalityType(userId);
            
            // Check for crisis indicators
            boolean hasCrisisIndicators = checkForCrisisIndicators(request.getMessage());
            
            String responseText;
            if (hasCrisisIndicators) {
                responseText = getCrisisResponse();
                logger.warn("Crisis indicators detected in fallback response for user: {}", userId);
            } else {
                responseText = FALLBACK_RESPONSES.getOrDefault(personalityType, FALLBACK_RESPONSES.get("supportive"));
            }
            
            // Build response
            AIChatResponse response = new AIChatResponse();
            response.setMessageId(UUID.randomUUID().toString());
            response.setResponse(responseText);
            response.setPersonalityUsed(personalityType);
            response.setEmotionDetected("concerned");
            response.setSuggestedTopics(Arrays.asList("general support", "professional help", "crisis resources"));
            response.setMetadata(Map.of(
                "fallback", true,
                "ai_unavailable", true,
                "confidence", 0.3
            ));
            response.setSafetyFiltered(false);
            response.setSafetyReason("fallback_response");
            response.setTimestamp(LocalDateTime.now());
            response.setTokensUsed(0);
            response.setConfidence(0.3);
            response.setFollowUpQuestions(generateFallbackFollowUpQuestions(hasCrisisIndicators));
            response.setConversationContext("fallback");
            response.setNeedsHumanReview(hasCrisisIndicators);
            response.setReviewReason(hasCrisisIndicators ? "crisis_indicators_detected" : "ai_unavailable");
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating fallback chat response: {}", e.getMessage(), e);
            return createEmergencyFallbackResponse();
        }
    }
    
    /**
     * Generate fallback moderation result
     */
    public ModerationResult generateFallbackModerationResult(String content, String contentType) {
        logger.info("Generating fallback moderation result for content type: {}", contentType);
        
        try {
            // Basic content analysis without AI
            boolean isSafe = performBasicContentAnalysis(content);
            String reason = isSafe ? "Basic analysis - content appears safe" : "Basic analysis - potential concerns detected";
            
            ModerationResult result = new ModerationResult();
            result.setSafe(isSafe);
            result.setReason(reason);
            result.setConfidence(0.5); // Lower confidence for fallback
            result.setCategories(isSafe ? Arrays.asList("safe") : Arrays.asList("review_required"));
            result.setSeverity(isSafe ? ModerationResult.SeverityLevel.LOW : ModerationResult.SeverityLevel.MEDIUM);
            result.setActionTaken(isSafe ? "approved" : "flagged_for_review");
            result.setTokensUsed(0);
            result.setCost(0.0);
            result.setProcessingTimeMs(0);
            result.setTimestamp(LocalDateTime.now());
            result.setFallback(true);
            result.setAiUnavailable(true);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error generating fallback moderation result: {}", e.getMessage(), e);
            return createSafeFallbackModerationResult();
        }
    }
    
    /**
     * Generate fallback personality profile
     */
    public UserPersonalityProfile generateFallbackPersonalityProfile(UUID userId) {
        logger.info("Generating fallback personality profile for user: {}", userId);
        
        UserPersonalityProfile profile = new UserPersonalityProfile();
        profile.setUserId(userId.toString());
        profile.setPreferredPersonalityType("supportive");
        profile.setCommunicationStyle("gentle");
        profile.setInterests(Arrays.asList("general support"));
        profile.setTriggers(Arrays.asList());
        profile.setPreferences(Map.of("fallback_mode", true));
        profile.setEmotionalState("neutral");
        profile.setPreferredLanguage("en");
        profile.setConversationCount(0);
        profile.setLastUpdated(LocalDateTime.now());
        profile.setTopicEngagement(Map.of());
        profile.setPreferredResponseLength(Arrays.asList("medium"));
        profile.setEnableHumor(false);
        profile.setEnableEmpathy(true);
        profile.setTimezone("UTC");
        profile.setCulturalContext("general");
        
        return profile;
    }
    
    /**
     * Get user personality type for fallback responses
     */
    private String getUserPersonalityType(UUID userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                // Simple heuristic based on user data
                User user = userOpt.get();
                // Default to supportive for fallback
                return "supportive";
            }
        } catch (Exception e) {
            logger.warn("Error getting user personality type: {}", e.getMessage());
        }
        return "supportive";
    }
    
    /**
     * Check for crisis indicators in message
     */
    private boolean checkForCrisisIndicators(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("suicide") ||
               lowerMessage.contains("kill myself") ||
               lowerMessage.contains("end it all") ||
               lowerMessage.contains("not worth living") ||
               lowerMessage.contains("hurt myself") ||
               lowerMessage.contains("self harm");
    }
    
    /**
     * Get crisis response
     */
    private String getCrisisResponse() {
        Random random = new Random();
        return CRISIS_RESPONSES.get(random.nextInt(CRISIS_RESPONSES.size()));
    }
    
    /**
     * Generate fallback follow-up questions
     */
    private List<String> generateFallbackFollowUpQuestions(boolean hasCrisisIndicators) {
        if (hasCrisisIndicators) {
            return Arrays.asList(
                "Are you safe right now?",
                "Do you have someone you can talk to?",
                "Would you like help finding crisis resources?"
            );
        } else {
            return Arrays.asList(
                "How are you feeling right now?",
                "Is there anything specific I can help you with?",
                "Would you like to talk about what's on your mind?"
            );
        }
    }
    
    /**
     * Perform basic content analysis without AI
     */
    private boolean performBasicContentAnalysis(String content) {
        if (content == null || content.trim().isEmpty()) {
            return true;
        }
        
        String lowerContent = content.toLowerCase();
        
        // Check for obvious harmful content
        List<String> harmfulPatterns = Arrays.asList(
            "kill", "suicide", "self harm", "violence", "hate", "abuse"
        );
        
        for (String pattern : harmfulPatterns) {
            if (lowerContent.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create emergency fallback response
     */
    private AIChatResponse createEmergencyFallbackResponse() {
        AIChatResponse response = new AIChatResponse();
        response.setMessageId(UUID.randomUUID().toString());
        response.setResponse("I'm sorry, but I'm experiencing technical difficulties. " +
                           "Please try again later or contact support if you need immediate assistance.");
        response.setPersonalityUsed("supportive");
        response.setEmotionDetected("neutral");
        response.setSuggestedTopics(Arrays.asList("technical support"));
        response.setMetadata(Map.of("emergency_fallback", true));
        response.setSafetyFiltered(false);
        response.setSafetyReason("emergency_fallback");
        response.setTimestamp(LocalDateTime.now());
        response.setTokensUsed(0);
        response.setConfidence(0.1);
        response.setFollowUpQuestions(Arrays.asList("Would you like to try again?"));
        response.setConversationContext("emergency_fallback");
        response.setNeedsHumanReview(false);
        response.setReviewReason("emergency_fallback");
        
        return response;
    }
    
    /**
     * Create safe fallback moderation result
     */
    private ModerationResult createSafeFallbackModerationResult() {
        ModerationResult result = new ModerationResult();
        result.setSafe(true);
        result.setReason("Emergency fallback - content approved by default");
        result.setConfidence(0.0);
        result.setCategories(Arrays.asList("safe"));
        result.setSeverity(ModerationResult.SeverityLevel.LOW);
        result.setActionTaken("approved");
        result.setTokensUsed(0);
        result.setCost(0.0);
        result.setProcessingTimeMs(0);
        result.setTimestamp(LocalDateTime.now());
        result.setFallback(true);
        result.setAiUnavailable(true);
        
        return result;
    }
    
    /**
     * Check if AI services are available
     * Uses health check service if available, otherwise falls back to configuration check
     */
    public boolean isAIServiceAvailable() {
        try {
            // Use health check service if available
            if (healthCheckService != null) {
                return healthCheckService.isAIServiceAvailable();
            }
            
            // Fallback to basic configuration check
            return performBasicHealthCheck();
            
        } catch (Exception e) {
            logger.warn("Error checking AI service availability: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform basic health check using configuration
     */
    private boolean performBasicHealthCheck() {
        // First check if AI is enabled in configuration
        if (!aiEnabled) {
            logger.debug("AI services disabled in configuration");
            return false;
        }
        
        // Check if required configuration is present
        if (deepSeekBaseUrl == null || deepSeekBaseUrl.trim().isEmpty() ||
            deepSeekApiKey == null || deepSeekApiKey.trim().isEmpty()) {
            logger.debug("AI service configuration incomplete");
            return false;
        }
        
        // Basic configuration validation passed
        logger.debug("AI service configuration appears valid");
        return true;
    }
    
    
    /**
     * Get fallback service status
     */
    public Map<String, Object> getFallbackServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean aiAvailable = isAIServiceAvailable();
        
        status.put("available", true);
        status.put("ai_services_available", aiAvailable);
        status.put("ai_enabled", aiEnabled);
        status.put("ai_configured", isAIConfigured());
        status.put("fallback_responses_configured", !FALLBACK_RESPONSES.isEmpty());
        status.put("crisis_responses_configured", !CRISIS_RESPONSES.isEmpty());
        status.put("fallback_mode_active", !aiAvailable);
        status.put("timestamp", LocalDateTime.now());
        
        return status;
    }
    
    /**
     * Check if AI service is properly configured
     */
    private boolean isAIConfigured() {
        return deepSeekBaseUrl != null && !deepSeekBaseUrl.trim().isEmpty() &&
               deepSeekApiKey != null && !deepSeekApiKey.trim().isEmpty();
    }
    
    /**
     * Classify a message for crisis detection and response categorization
     */
    public ClassificationResult classifyMessage(String message) {
        logger.info("Classifying message for fallback response");
        
        try {
            String lowerMessage = message.toLowerCase();
            
            // Crisis detection
            if (checkForCrisisIndicators(message)) {
                return new ClassificationResult(
                    "CRISIS", 
                    1.0, 
                    "Crisis indicators detected - immediate intervention required",
                    "I'm deeply concerned about what you're sharing. Your life has value, and there are people who want to help you through this difficult time. Please reach out to a crisis helpline immediately: National Suicide Prevention Lifeline at 988 (US) or your local emergency services. You don't have to face this alone."
                );
            }
            
            // Emotional support detection
            if (lowerMessage.contains("sad") || lowerMessage.contains("lonely") || 
                lowerMessage.contains("depressed") || lowerMessage.contains("anxious") ||
                lowerMessage.contains("worried") || lowerMessage.contains("stressed")) {
                return new ClassificationResult(
                    "EMOTIONAL_SUPPORT", 
                    0.8, 
                    "Emotional support needed",
                    "I can hear that you're going through a tough time right now. While our AI features are temporarily unavailable, I want you to know that your feelings are valid and important. Consider reaching out to a trusted friend, family member, or mental health professional for support."
                );
            }
            
            // General category
            return new ClassificationResult(
                "GENERAL", 
                0.5, 
                "General message classification",
                "I'm here to help, though our AI features are currently unavailable. How can I assist you today?"
            );
            
        } catch (Exception e) {
            logger.error("Error classifying message: {}", e.getMessage(), e);
            return new ClassificationResult(
                "GENERAL", 
                0.3, 
                "Classification error - defaulting to general",
                "I'm here to help, though our AI features are currently unavailable. How can I assist you today?"
            );
        }
    }
    
    /**
     * Make a moderation decision based on content and confidence
     */
    public ModerationDecision makeModerationDecision(String content, double confidence) {
        logger.info("Making moderation decision with confidence: {}", confidence);
        
        try {
            // Basic content analysis
            boolean isSafe = performBasicContentAnalysis(content);
            
            ModerationAction action;
            String reason;
            
            if (isSafe && confidence > 0.7) {
                action = ModerationAction.ALLOW;
                reason = "Content appears safe with high confidence";
            } else if (isSafe && confidence > 0.5) {
                action = ModerationAction.ALLOW;
                reason = "Content appears safe with moderate confidence";
            } else if (confidence > 0.3) {
                action = ModerationAction.FLAG_FOR_REVIEW;
                reason = "Content requires manual review";
            } else {
                action = ModerationAction.BLOCK;
                reason = "Content appears unsafe with low confidence";
            }
            
            return new ModerationDecision(action, reason, confidence);
            
        } catch (Exception e) {
            logger.error("Error making moderation decision: {}", e.getMessage(), e);
            return new ModerationDecision(ModerationAction.FLAG_FOR_REVIEW, "Error in moderation analysis", 0.0);
        }
    }
    
    /**
     * Provide a degraded response when AI services are unavailable
     */
    public String provideDegradedResponse(String userMessage, UUID userId) {
        logger.info("Providing degraded response for user: {}", userId);
        
        try {
            // Get user personality for appropriate response style
            String personalityType = getUserPersonalityType(userId);
            
            // Check for crisis indicators
            boolean hasCrisisIndicators = checkForCrisisIndicators(userMessage);
            
            if (hasCrisisIndicators) {
                return "I'm deeply concerned about what you're sharing. Your life has value, and there are people who want to help you through this difficult time. Please reach out to a crisis helpline immediately: National Suicide Prevention Lifeline at 988 (US) or your local emergency services. You don't have to face this alone.";
            }
            
            // Use appropriate fallback response based on personality
            String baseResponse = FALLBACK_RESPONSES.getOrDefault(personalityType, FALLBACK_RESPONSES.get("supportive"));
            
            return baseResponse + " (Simplified mode - AI features temporarily unavailable)";
            
        } catch (Exception e) {
            logger.error("Error providing degraded response: {}", e.getMessage(), e);
            return "I'm here to help, though our AI features are currently unavailable. How can I assist you today? (Simplified mode)";
        }
    }
    
    /**
     * Inner class for message classification results
     */
    public static class ClassificationResult {
        private final String category;
        private final double confidence;
        private final String reason;
        private final String fallbackResponse;
        
        public ClassificationResult(String category, double confidence, String reason, String fallbackResponse) {
            this.category = category;
            this.confidence = confidence;
            this.reason = reason;
            this.fallbackResponse = fallbackResponse;
        }
        
        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
        public String getFallbackResponse() { return fallbackResponse; }
    }
    
    /**
     * Inner class for moderation decisions
     */
    public static class ModerationDecision {
        private final ModerationAction action;
        private final String reason;
        private final double confidence;
        
        public ModerationDecision(ModerationAction action, String reason, double confidence) {
            this.action = action;
            this.reason = reason;
            this.confidence = confidence;
        }
        
        public ModerationAction getAction() { return action; }
        public String getReason() { return reason; }
        public double getConfidence() { return confidence; }
    }
    
    /**
     * Enum for moderation actions
     */
    public enum ModerationAction {
        ALLOW, FLAG_FOR_REVIEW, BLOCK
    }
}
