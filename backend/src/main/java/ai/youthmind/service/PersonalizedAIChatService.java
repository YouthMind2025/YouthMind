package ai.youthmind.service;

import ai.youthmind.entity.MemoryItem;
import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PersonalizedAIChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalizedAIChatService.class);
    
    @Autowired
    private RAGContextBuilderService ragContextBuilder;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Generate personalized AI response with memory integration
     */
    public PersonalizedResponse generatePersonalizedResponse(UUID userId, String userMessage, 
                                                           String conversationHistory) {
        try {
            // Build RAG context
            String ragContext = ragContextBuilder.buildRAGContext(userId, userMessage, conversationHistory);
            
            // Get structured context facts
            Map<String, Object> contextFacts = ragContextBuilder.getContextFacts(userId, userMessage);
            
            // Build personalized prompt
            String personalizedPrompt = buildPersonalizedPrompt(userId, userMessage, ragContext, contextFacts);
            
            // Generate AI response
            String aiResponse = generateAIResponse(personalizedPrompt, contextFacts);
            
            // Validate and enhance response
            PersonalizedResponse response = validateAndEnhanceResponse(userId, userMessage, aiResponse, contextFacts);
            
            // Store interaction in memory
            storeInteractionMemory(userId, userMessage, response, contextFacts);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to generate personalized response for user {}: {}", userId, e.getMessage());
            return generateFallbackResponse(userId, userMessage);
        }
    }
    
    /**
     * Build personalized prompt with user context
     */
    private String buildPersonalizedPrompt(UUID userId, String userMessage, String ragContext, 
                                         Map<String, Object> contextFacts) {
        StringBuilder prompt = new StringBuilder();
        
        // System prompt with personalization
        prompt.append("You are a compassionate AI mental health support assistant for YouthMind. ");
        prompt.append("Provide personalized, empathetic responses based on the user's context.\n\n");
        
        // User context
        prompt.append("USER CONTEXT:\n");
        prompt.append(ragContext).append("\n\n");
        
        // Current interaction
        prompt.append("CURRENT INTERACTION:\n");
        prompt.append("User Message: ").append(userMessage).append("\n\n");
        
        // Response guidelines
        prompt.append("RESPONSE GUIDELINES:\n");
        prompt.append("- Be empathetic and understanding\n");
        prompt.append("- Reference user's context when relevant\n");
        prompt.append("- Provide actionable support\n");
        prompt.append("- Keep response under 300 words\n");
        prompt.append("- Use the user's preferred communication style\n");
        prompt.append("- Include follow-up questions when appropriate\n\n");
        
        // JSON output format
        prompt.append("REQUIRED OUTPUT FORMAT (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"response\": \"Your empathetic response here\",\n");
        prompt.append("  \"emotion_detected\": \"detected_emotion\",\n");
        prompt.append("  \"support_type\": \"emotional|practical|informational\",\n");
        prompt.append("  \"follow_up_questions\": [\"question1\", \"question2\"],\n");
        prompt.append("  \"suggested_actions\": [\"action1\", \"action2\"],\n");
        prompt.append("  \"crisis_indicators\": false,\n");
        prompt.append("  \"confidence_score\": 0.85\n");
        prompt.append("}\n\n");
        
        prompt.append("Generate your response now:");
        
        return prompt.toString();
    }
    
    /**
     * Generate AI response using external AI service
     */
    private String generateAIResponse(String prompt, Map<String, Object> contextFacts) {
        try {
            // Call AI service with personalized prompt
            Map<String, Object> request = new HashMap<>();
            request.put("prompt", prompt);
            request.put("context", contextFacts);
            request.put("max_tokens", 500);
            request.put("temperature", 0.7);
            request.put("response_format", "json");
            
            return aiServiceClient.generateResponse(request);
            
        } catch (Exception e) {
            logger.error("AI service call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable", e);
        }
    }
    
    /**
     * Validate and enhance AI response
     */
    private PersonalizedResponse validateAndEnhanceResponse(UUID userId, String userMessage, 
                                                          String aiResponse, Map<String, Object> contextFacts) {
        try {
            // Parse JSON response
            Map<String, Object> responseData = objectMapper.readValue(aiResponse, Map.class);
            
            PersonalizedResponse response = new PersonalizedResponse();
            response.setResponse((String) responseData.get("response"));
            response.setEmotionDetected((String) responseData.get("emotion_detected"));
            response.setSupportType((String) responseData.get("support_type"));
            response.setFollowUpQuestions((List<String>) responseData.get("follow_up_questions"));
            response.setSuggestedActions((List<String>) responseData.get("suggested_actions"));
            response.setCrisisIndicators((Boolean) responseData.getOrDefault("crisis_indicators", false));
            response.setConfidenceScore(((Number) responseData.getOrDefault("confidence_score", 0.5)).doubleValue());
            
            // Validate response quality
            validateResponseQuality(response);
            
            // Enhance with personalization
            enhanceWithPersonalization(userId, response, contextFacts);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to parse AI response: {}", e.getMessage());
            return createFallbackResponse(userMessage);
        }
    }
    
    /**
     * Validate response quality and safety
     */
    private void validateResponseQuality(PersonalizedResponse response) {
        // Check response length
        if (response.getResponse() == null || response.getResponse().trim().isEmpty()) {
            throw new RuntimeException("Empty AI response");
        }
        
        if (response.getResponse().length() < 10) {
            throw new RuntimeException("AI response too short");
        }
        
        if (response.getResponse().length() > 2000) {
            response.setResponse(response.getResponse().substring(0, 1997) + "...");
        }
        
        // Check for inappropriate content
        String lowerResponse = response.getResponse().toLowerCase();
        if (lowerResponse.contains("i am not") || lowerResponse.contains("i cannot") ||
            lowerResponse.contains("as an ai")) {
            throw new RuntimeException("AI response contains robotic language");
        }
        
        // Ensure empathetic tone
        if (!containsEmpathyIndicators(response.getResponse())) {
            // Add empathy if missing
            response.setResponse("I understand this might be difficult. " + response.getResponse());
        }
    }
    
    /**
     * Enhance response with personalization based on user context
     */
    private void enhanceWithPersonalization(UUID userId, PersonalizedResponse response, 
                                          Map<String, Object> contextFacts) {
        try {
            // Get user name for personalization
            String userName = (String) contextFacts.get("userName");
            if (userName != null && !response.getResponse().contains(userName)) {
                // Occasionally use user's name (not every response)
                if (Math.random() < 0.3) {
                    response.setResponse(response.getResponse().replaceFirst("\\.", ", " + userName + "."));
                }
            }
            
            // Add relevant memories as context
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> memories = (List<Map<String, Object>>) contextFacts.get("relevantMemories");
            if (memories != null && !memories.isEmpty() && Math.random() < 0.4) {
                String memoryReference = buildMemoryReference(memories);
                if (memoryReference != null) {
                    response.setSuggestedActions(
                        response.getSuggestedActions() != null ? 
                        new ArrayList<>(response.getSuggestedActions()) : new ArrayList<>()
                    );
                    response.getSuggestedActions().add("Reflect on: " + memoryReference);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to enhance response with personalization: {}", e.getMessage());
        }
    }
    
    /**
     * Store interaction in memory for future personalization
     */
    private void storeInteractionMemory(UUID userId, String userMessage, PersonalizedResponse response, 
                                      Map<String, Object> contextFacts) {
        try {
            // Store successful interaction pattern
            String key = "interaction_" + System.currentTimeMillis();
            String content = String.format("User: %s\nAI: %s\nEmotion: %s\nSupport: %s", 
                                         userMessage.substring(0, Math.min(200, userMessage.length())),
                                         response.getResponse().substring(0, Math.min(200, response.getResponse().length())),
                                         response.getEmotionDetected(),
                                         response.getSupportType());
            
            Map<String, Object> context = new HashMap<>();
            context.put("emotion_detected", response.getEmotionDetected());
            context.put("support_type", response.getSupportType());
            context.put("confidence_score", response.getConfidenceScore());
            context.put("crisis_indicators", response.getCrisisIndicators());
            context.put("interaction_timestamp", LocalDateTime.now().toString());
            
            memoryService.storeMemory(userId, MemoryItem.MemoryType.INTERACTION_HISTORY, 
                                    key, content, context);
            
            // Update engagement weights based on response quality
            if (response.getConfidenceScore() > 0.8) {
                memoryService.updateEngagementWeight(userId, key, "POSITIVE_INTERACTION");
            }
            
        } catch (Exception e) {
            logger.warn("Failed to store interaction memory: {}", e.getMessage());
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    private boolean containsEmpathyIndicators(String response) {
        String lower = response.toLowerCase();
        return lower.contains("understand") || lower.contains("feel") || 
               lower.contains("sounds") || lower.contains("that must") ||
               lower.contains("i hear") || lower.contains("it's okay") ||
               lower.contains("you're not alone");
    }
    
    private String buildMemoryReference(List<Map<String, Object>> memories) {
        if (memories.isEmpty()) return null;
        
        Map<String, Object> firstMemory = memories.get(0);
        String content = (String) firstMemory.get("content");
        
        if (content != null && content.length() > 50) {
            return content.substring(0, 47) + "...";
        }
        
        return content;
    }
    
    private PersonalizedResponse generateFallbackResponse(UUID userId, String userMessage) {
        PersonalizedResponse fallback = new PersonalizedResponse();
        fallback.setResponse("I understand you're reaching out for support. While I'm processing your message, " +
                           "please know that you're not alone and your feelings are valid.");
        fallback.setEmotionDetected("UNKNOWN");
        fallback.setSupportType("emotional");
        fallback.setFollowUpQuestions(Arrays.asList("How are you feeling right now?", 
                                                   "What would be most helpful for you today?"));
        fallback.setCrisisIndicators(false);
        fallback.setConfidenceScore(0.3);
        
        return fallback;
    }
    
    private PersonalizedResponse createFallbackResponse(String userMessage) {
        return generateFallbackResponse(null, userMessage);
    }
    
    // ==================== RESPONSE DATA CLASS ====================
    
    public static class PersonalizedResponse {
        private String response;
        private String emotionDetected;
        private String supportType;
        private List<String> followUpQuestions;
        private List<String> suggestedActions;
        private Boolean crisisIndicators;
        private Double confidenceScore;
        private Map<String, Object> metadata;
        
        // Constructors
        public PersonalizedResponse() {
            this.followUpQuestions = new ArrayList<>();
            this.suggestedActions = new ArrayList<>();
            this.crisisIndicators = false;
            this.confidenceScore = 0.5;
            this.metadata = new HashMap<>();
        }
        
        // Getters and setters
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        
        public String getEmotionDetected() { return emotionDetected; }
        public void setEmotionDetected(String emotionDetected) { this.emotionDetected = emotionDetected; }
        
        public String getSupportType() { return supportType; }
        public void setSupportType(String supportType) { this.supportType = supportType; }
        
        public List<String> getFollowUpQuestions() { return followUpQuestions; }
        public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions; }
        
        public List<String> getSuggestedActions() { return suggestedActions; }
        public void setSuggestedActions(List<String> suggestedActions) { this.suggestedActions = suggestedActions; }
        
        public Boolean getCrisisIndicators() { return crisisIndicators; }
        public void setCrisisIndicators(Boolean crisisIndicators) { this.crisisIndicators = crisisIndicators; }
        
        public Double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
}
