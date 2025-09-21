package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Conversation;
import ai.youthmind.entity.Message;
import ai.youthmind.entity.User;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.MessageRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.util.NLPAnalyzer;
import ai.youthmind.util.PersonalityAdapter;
import ai.youthmind.util.SafetyFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AIChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIChatService.class);
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private NLPAnalyzer nlpAnalyzer;
    
    @Autowired
    private PersonalityAdapter personalityAdapter;
    
    @Autowired
    private SafetyFilter safetyFilter;
    
    @Autowired
    private AIFallbackService fallbackService;
    
    @Autowired
    private AIRequestBatchingService requestBatchingService;
    
    @Autowired
    private AIAsyncProcessingService asyncProcessingService;
    
    @Autowired
    private AIDatabaseOptimizationService databaseOptimizationService;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Personality system prompts
    private static final Map<String, String> PERSONALITY_PROMPTS = Map.of(
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
    
    /**
     * Process AI chat request asynchronously for better performance
     */
    public CompletableFuture<AIChatResponse> processChatRequestAsync(AIChatRequest request, UUID userId) {
        return asyncProcessingService.processChatRequestAsync(request, userId);
    }
    
    /**
     * Process AI chat request with batching for optimal performance
     */
    public CompletableFuture<String> processBatchedChatRequest(AIChatRequest request, UUID userId) {
        return requestBatchingService.processBatchedAIRequest(request, userId);
    }
    
    /**
     * Process AI chat request with enhanced features
     */
    @Transactional
    public AIChatResponse processChatRequest(AIChatRequest request, UUID userId) {
        try {
            logger.info("Processing AI chat request for user: {}", userId);
            
            // 1. Get user personality profile
            UserPersonalityProfile personalityProfile = getUserPersonalityProfile(userId);
            
            // 2. Analyze the input message
            Map<String, Object> nlpAnalysis = nlpAnalyzer.analyzeMessage(request.getMessage());
            
            // 3. Apply safety filtering
            AISafetyFilterResult safetyResult = safetyFilter.filterMessage(
                request.getMessage(), 
                personalityProfile, 
                nlpAnalysis
            );
            
            if (!safetyResult.isSafe()) {
                return createSafetyFilteredResponse(safetyResult, request);
            }
            
            // 4. Get conversation context
            String conversationContext = getConversationContext(request.getConversationId(), userId);
            
            // 5. Adapt personality based on user profile and context
            String adaptedPersonality = personalityAdapter.adaptPersonality(
                personalityProfile, 
                nlpAnalysis, 
                conversationContext
            );
            
            // 6. Generate AI response
            String aiResponse = generateAIResponse(
                request, 
                personalityProfile, 
                adaptedPersonality, 
                conversationContext,
                nlpAnalysis
            );
            
            // 7. Apply safety filtering to AI response
            AISafetyFilterResult responseSafetyResult = safetyFilter.filterMessage(
                aiResponse, 
                personalityProfile, 
                Map.of("isAIResponse", true)
            );
            
            if (!responseSafetyResult.isSafe()) {
                aiResponse = generateSafeAlternativeResponse(request, personalityProfile, adaptedPersonality);
            }
            
            // 8. Save the conversation
            Message userMessage = saveUserMessage(request, userId);
            Message aiMessage = saveAIMessage(aiResponse, request.getConversationId(), adaptedPersonality);
            
            // 9. Update user personality profile
            updateUserPersonalityProfile(userId, request, nlpAnalysis, aiResponse);
            
            // 10. Generate response
            return buildChatResponse(aiMessage, personalityProfile, adaptedPersonality, nlpAnalysis, responseSafetyResult);
            
        } catch (Exception e) {
            logger.error("Error processing AI chat request: {}", e.getMessage(), e);
            
            // Try fallback service if AI is unavailable
            try {
                logger.info("Attempting fallback response for user: {}", userId);
                return fallbackService.generateFallbackChatResponse(request, userId);
            } catch (Exception fallbackError) {
                logger.error("Fallback service also failed: {}", fallbackError.getMessage(), fallbackError);
                return createErrorResponse("Sorry, I encountered an error. Please try again.", request);
            }
        }
    }
    
    /**
     * Get or create user personality profile
     */
    public UserPersonalityProfile getUserPersonalityProfile(UUID userId) {
        return userPersonalityService.getUserPersonalityProfile(userId);
    }
    
    /**
     * Get conversation context for better AI responses using optimized database queries
     */
    private String getConversationContext(String conversationId, UUID userId) {
        try {
            UUID convId = UUID.fromString(conversationId);
            return databaseOptimizationService.getOptimizedConversationContext(convId, userId);
        } catch (Exception e) {
            logger.warn("Failed to get conversation context: {}", e.getMessage());
            return "New conversation";
        }
    }
    
    /**
     * Generate AI response using DeepSeek API with Redis caching
     */
    private String generateAIResponse(AIChatRequest request, UserPersonalityProfile personalityProfile, 
                                    String adaptedPersonality, String conversationContext, 
                                    Map<String, Object> nlpAnalysis) {
        
        // Create cache key based on request content and personality
        String cacheKey = createAIResponseCacheKey(request, personalityProfile, adaptedPersonality, nlpAnalysis);
        
        try {
            // Try to get from cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.debug("Cache hit for AI response: {}", cacheKey);
                return (String) cached;
            }
        } catch (Exception e) {
            logger.warn("Failed to get AI response from cache: {}", e.getMessage());
        }
        
        String systemPrompt = PERSONALITY_PROMPTS.get(adaptedPersonality);
        if (systemPrompt == null) {
            systemPrompt = PERSONALITY_PROMPTS.get("supportive");
        }
        
        // Enhance system prompt with user-specific information
        systemPrompt += "\n\nUser Profile:\n";
        systemPrompt += "- Preferred communication style: " + personalityProfile.getCommunicationStyle() + "\n";
        systemPrompt += "- Emotional state: " + personalityProfile.getEmotionalState() + "\n";
        systemPrompt += "- Interests: " + String.join(", ", personalityProfile.getInterests()) + "\n";
        systemPrompt += "- Enable humor: " + personalityProfile.isEnableHumor() + "\n";
        systemPrompt += "- Enable empathy: " + personalityProfile.isEnableEmpathy() + "\n";
        
        if (nlpAnalysis.containsKey("emotion")) {
            systemPrompt += "- Detected emotion: " + nlpAnalysis.get("emotion") + "\n";
        }
        
        // Build the user message with context
        String userMessage = "Context:\n" + conversationContext + "\n\nUser message: " + request.getMessage();
        
        // Create the API request
        DeepSeekModerationRequest.Message systemMsg = new DeepSeekModerationRequest.Message("system", systemPrompt);
        DeepSeekModerationRequest.Message userMsg = new DeepSeekModerationRequest.Message("user", userMessage);
        
        DeepSeekModerationRequest apiRequest = new DeepSeekModerationRequest(
            "deepseek-chat",
            Arrays.asList(systemMsg, userMsg),
            0.7, // Higher temperature for more creative responses
            500, // Max tokens for detailed responses
            false
        );
        
        try {
            // Call the AI service (reusing existing infrastructure)
            DeepSeekModerationResponse response = callDeepSeekAPI(apiRequest);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String aiResponse = response.getChoices().get(0).getMessage().getContent();
                logger.debug("Generated AI response: {}", aiResponse);
                
                // Cache the response for future use
                try {
                    redisTemplate.opsForValue().set(cacheKey, aiResponse, 30, TimeUnit.MINUTES);
                    logger.debug("Cached AI response: {}", cacheKey);
                } catch (Exception e) {
                    logger.warn("Failed to cache AI response: {}", e.getMessage());
                }
                
                return aiResponse;
            }
            
            logger.warn("Empty response from DeepSeek API, using fallback");
            return generateFallbackResponse(adaptedPersonality, nlpAnalysis);
            
        } catch (Exception e) {
            logger.error("Error generating AI response: {}", e.getMessage(), e);
            return generateFallbackResponse(adaptedPersonality, nlpAnalysis);
        }
    }
    
    /**
     * Create cache key for AI response based on request content and personality
     */
    private String createAIResponseCacheKey(AIChatRequest request, UserPersonalityProfile personalityProfile, 
                                          String adaptedPersonality, Map<String, Object> nlpAnalysis) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("ai_response:");
        keyBuilder.append("msg:").append(request.getMessage().hashCode());
        keyBuilder.append(":personality:").append(adaptedPersonality);
        keyBuilder.append(":style:").append(personalityProfile.getCommunicationStyle());
        keyBuilder.append(":emotion:").append(nlpAnalysis.getOrDefault("emotion", "neutral"));
        keyBuilder.append(":context:").append(request.getConversationId().hashCode());
        return keyBuilder.toString();
    }
    
    /**
     * Call DeepSeek API for chat completion
     */
    private DeepSeekModerationResponse callDeepSeekAPI(DeepSeekModerationRequest request) {
        try {
            // Use the existing AIServiceClient infrastructure
            return aiServiceClient.generateChatCompletion(request);
        } catch (Exception e) {
            logger.error("Error calling DeepSeek API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI response: " + e.getMessage());
        }
    }
    
    /**
     * Generate fallback response when AI service is unavailable
     */
    private String generateFallbackResponse(String personality, Map<String, Object> nlpAnalysis) {
        String emotion = (String) nlpAnalysis.getOrDefault("emotion", "neutral");
        
        switch (personality) {
            case "empathetic":
                return "I can sense you're going through something important. I'm here to listen and understand. " +
                       "What's weighing on your mind right now?";
            case "professional":
                return "I'm here to provide support and guidance. Could you help me understand what you're experiencing " +
                       "so I can offer the most appropriate assistance?";
            case "casual":
                return "Hey, I'm here for you! Whatever you're dealing with, we can work through it together. " +
                       "What's going on?";
            case "supportive":
            default:
                return "I'm here to support you through whatever you're facing. " +
                       "Take your time and share what's on your mind - I'm listening.";
        }
    }
    
    /**
     * Save user message to database
     */
    private Message saveUserMessage(AIChatRequest request, UUID userId) {
        Message message = new Message();
        message.setConversationId(UUID.fromString(request.getConversationId()));
        message.setText(request.getMessage());
        message.setSender("user");
        message.setTimestamp(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    /**
     * Save AI message to database
     */
    private Message saveAIMessage(String response, String conversationId, String personality) {
        Message message = new Message();
        message.setConversationId(UUID.fromString(conversationId));
        message.setText(response);
        message.setSender("ai");
        message.setTimestamp(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    /**
     * Update user personality profile based on interaction using DeepSeek API
     */
    private void updateUserPersonalityProfile(UUID userId, AIChatRequest request, 
                                            Map<String, Object> nlpAnalysis, String aiResponse) {
        try {
            logger.info("Updating personality profile for user: {}", userId);
            
            // Get current personality profile
            UserPersonalityProfile currentProfile = getUserPersonalityProfile(userId);
            
            // Get conversation history for context
            String conversationContext = getConversationContext(request.getConversationId(), userId);
            
            // Analyze user personality using DeepSeek API
            UserPersonalityProfile updatedProfile = analyzeUserPersonalityWithAI(
                currentProfile, request, nlpAnalysis, aiResponse, conversationContext
            );
            
            // Update the profile in cache and database
            String cacheKey = "user_personality:" + userId;
            redisTemplate.opsForValue().set(cacheKey, updatedProfile, 1, TimeUnit.HOURS);
            
            logger.info("Successfully updated personality profile for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error updating personality profile for user {}: {}", userId, e.getMessage(), e);
            // Don't throw exception - this is not critical for the main chat flow
        }
    }
    
    /**
     * Analyze user personality using DeepSeek API
     */
    private UserPersonalityProfile analyzeUserPersonalityWithAI(UserPersonalityProfile currentProfile, 
                                                               AIChatRequest request, 
                                                               Map<String, Object> nlpAnalysis, 
                                                               String aiResponse, 
                                                               String conversationContext) {
        try {
            // Create personality analysis prompt
            String analysisPrompt = buildPersonalityAnalysisPrompt(currentProfile, request, nlpAnalysis, aiResponse, conversationContext);
            
            // Create API request
            DeepSeekModerationRequest.Message systemMsg = new DeepSeekModerationRequest.Message("system", 
                "You are an AI personality analyst for YouthMind, a mental health platform for youth. " +
                "Analyze user interactions to understand their communication patterns, emotional tendencies, " +
                "and preferences. Provide insights to help personalize their AI chat experience.");
            
            DeepSeekModerationRequest.Message userMsg = new DeepSeekModerationRequest.Message("user", analysisPrompt);
            
            DeepSeekModerationRequest apiRequest = new DeepSeekModerationRequest(
                "deepseek-chat",
                Arrays.asList(systemMsg, userMsg),
                0.3, // Lower temperature for consistent analysis
                800, // More tokens for detailed analysis
                false
            );
            
            // Call DeepSeek API
            DeepSeekModerationResponse response = callDeepSeekAPI(apiRequest);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String analysisResult = response.getChoices().get(0).getMessage().getContent();
                return parsePersonalityAnalysis(analysisResult, currentProfile, nlpAnalysis);
            }
            
            // Fallback to basic analysis if API fails
            return performBasicPersonalityUpdate(currentProfile, nlpAnalysis);
            
        } catch (Exception e) {
            logger.error("Error analyzing personality with AI: {}", e.getMessage(), e);
            return performBasicPersonalityUpdate(currentProfile, nlpAnalysis);
        }
    }
    
    /**
     * Build personality analysis prompt for DeepSeek API
     */
    private String buildPersonalityAnalysisPrompt(UserPersonalityProfile currentProfile, 
                                                 AIChatRequest request, 
                                                 Map<String, Object> nlpAnalysis, 
                                                 String aiResponse, 
                                                 String conversationContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Analyze this user interaction and provide personality insights:\n\n");
        
        prompt.append("CURRENT USER PROFILE:\n");
        prompt.append("- Preferred Personality: ").append(currentProfile.getPreferredPersonalityType()).append("\n");
        prompt.append("- Communication Style: ").append(currentProfile.getCommunicationStyle()).append("\n");
        prompt.append("- Emotional State: ").append(currentProfile.getEmotionalState()).append("\n");
        prompt.append("- Interests: ").append(String.join(", ", currentProfile.getInterests())).append("\n");
        prompt.append("- Enable Humor: ").append(currentProfile.isEnableHumor()).append("\n");
        prompt.append("- Enable Empathy: ").append(currentProfile.isEnableEmpathy()).append("\n");
        prompt.append("- Conversation Count: ").append(currentProfile.getConversationCount()).append("\n\n");
        
        prompt.append("CURRENT INTERACTION:\n");
        prompt.append("- User Message: ").append(request.getMessage()).append("\n");
        prompt.append("- Detected Emotion: ").append(nlpAnalysis.getOrDefault("emotion", "unknown")).append("\n");
        prompt.append("- Sentiment: ").append(nlpAnalysis.getOrDefault("sentiment", "neutral")).append("\n");
        prompt.append("- Urgency Level: ").append(nlpAnalysis.getOrDefault("urgency", "normal")).append("\n");
        prompt.append("- Language Complexity: ").append(nlpAnalysis.getOrDefault("complexity", "medium")).append("\n");
        prompt.append("- AI Response: ").append(aiResponse).append("\n\n");
        
        prompt.append("CONVERSATION CONTEXT:\n").append(conversationContext).append("\n\n");
        
        prompt.append("Please analyze and provide updated personality insights in this JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"preferredPersonalityType\": \"supportive|professional|casual|empathetic\",\n");
        prompt.append("  \"communicationStyle\": \"gentle|direct|casual|formal\",\n");
        prompt.append("  \"emotionalState\": \"current emotional state\",\n");
        prompt.append("  \"interests\": [\"list\", \"of\", \"interests\"],\n");
        prompt.append("  \"enableHumor\": true/false,\n");
        prompt.append("  \"enableEmpathy\": true/false,\n");
        prompt.append("  \"preferredResponseLength\": [\"short|medium|long\"],\n");
        prompt.append("  \"culturalContext\": \"general|specific culture\",\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"reasoning\": \"explanation of changes\"\n");
        prompt.append("}\n\n");
        
        prompt.append("Focus on:\n");
        prompt.append("1. Communication patterns and preferences\n");
        prompt.append("2. Emotional tendencies and triggers\n");
        prompt.append("3. Response style preferences\n");
        prompt.append("4. Cultural and personal context\n");
        prompt.append("5. Mental health support needs\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse DeepSeek API response for personality analysis
     */
    private UserPersonalityProfile parsePersonalityAnalysis(String analysisResult, 
                                                           UserPersonalityProfile currentProfile, 
                                                           Map<String, Object> nlpAnalysis) {
        try {
            // Try to extract JSON from the response
            String jsonPart = extractJsonFromResponse(analysisResult);
            
            if (jsonPart != null) {
                // Parse JSON response
                Map<String, Object> analysis = objectMapper.readValue(jsonPart, Map.class);
                
                // Update profile with AI insights
                UserPersonalityProfile updatedProfile = new UserPersonalityProfile();
                updatedProfile.setUserId(currentProfile.getUserId());
                updatedProfile.setPreferredPersonalityType((String) analysis.getOrDefault("preferredPersonalityType", currentProfile.getPreferredPersonalityType()));
                updatedProfile.setCommunicationStyle((String) analysis.getOrDefault("communicationStyle", currentProfile.getCommunicationStyle()));
                updatedProfile.setEmotionalState((String) analysis.getOrDefault("emotionalState", (String) nlpAnalysis.getOrDefault("emotion", "neutral")));
                updatedProfile.setInterests((List<String>) analysis.getOrDefault("interests", currentProfile.getInterests()));
                updatedProfile.setEnableHumor((Boolean) analysis.getOrDefault("enableHumor", currentProfile.isEnableHumor()));
                updatedProfile.setEnableEmpathy((Boolean) analysis.getOrDefault("enableEmpathy", currentProfile.isEnableEmpathy()));
                updatedProfile.setPreferredResponseLength((List<String>) analysis.getOrDefault("preferredResponseLength", currentProfile.getPreferredResponseLength()));
                updatedProfile.setCulturalContext((String) analysis.getOrDefault("culturalContext", currentProfile.getCulturalContext()));
                updatedProfile.setConversationCount(currentProfile.getConversationCount() + 1);
                updatedProfile.setLastUpdated(LocalDateTime.now());
                
                // Preserve existing data
                updatedProfile.setTriggers(currentProfile.getTriggers());
                updatedProfile.setPreferences(currentProfile.getPreferences());
                updatedProfile.setTopicEngagement(currentProfile.getTopicEngagement());
                updatedProfile.setTimezone(currentProfile.getTimezone());
                updatedProfile.setPreferredLanguage(currentProfile.getPreferredLanguage());
                
                logger.info("Updated personality profile with AI analysis for user: {}", currentProfile.getUserId());
                return updatedProfile;
                
            } else {
                logger.warn("Could not extract JSON from AI personality analysis, using basic update");
                return performBasicPersonalityUpdate(currentProfile, nlpAnalysis);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing personality analysis: {}", e.getMessage(), e);
            return performBasicPersonalityUpdate(currentProfile, nlpAnalysis);
        }
    }
    
    /**
     * Extract JSON from AI response
     */
    private String extractJsonFromResponse(String response) {
        try {
            // Look for JSON block in the response
            int startIndex = response.indexOf("{");
            int endIndex = response.lastIndexOf("}");
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                return response.substring(startIndex, endIndex + 1);
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("Error extracting JSON from response: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Perform basic personality update without AI analysis
     */
    private UserPersonalityProfile performBasicPersonalityUpdate(UserPersonalityProfile currentProfile, 
                                                               Map<String, Object> nlpAnalysis) {
        UserPersonalityProfile updatedProfile = new UserPersonalityProfile();
        
        // Copy current profile
        updatedProfile.setUserId(currentProfile.getUserId());
        updatedProfile.setPreferredPersonalityType(currentProfile.getPreferredPersonalityType());
        updatedProfile.setCommunicationStyle(currentProfile.getCommunicationStyle());
        updatedProfile.setInterests(currentProfile.getInterests());
        updatedProfile.setTriggers(currentProfile.getTriggers());
        updatedProfile.setPreferences(currentProfile.getPreferences());
        updatedProfile.setEnableHumor(currentProfile.isEnableHumor());
        updatedProfile.setEnableEmpathy(currentProfile.isEnableEmpathy());
        updatedProfile.setPreferredResponseLength(currentProfile.getPreferredResponseLength());
        updatedProfile.setCulturalContext(currentProfile.getCulturalContext());
        updatedProfile.setTimezone(currentProfile.getTimezone());
        updatedProfile.setPreferredLanguage(currentProfile.getPreferredLanguage());
        updatedProfile.setTopicEngagement(currentProfile.getTopicEngagement());
        
        // Update basic fields
        updatedProfile.setEmotionalState((String) nlpAnalysis.getOrDefault("emotion", currentProfile.getEmotionalState()));
        updatedProfile.setConversationCount(currentProfile.getConversationCount() + 1);
        updatedProfile.setLastUpdated(LocalDateTime.now());
        
        // Simple pattern-based updates
        String emotion = (String) nlpAnalysis.getOrDefault("emotion", "neutral");
        if ("anxious".equals(emotion) || "sad".equals(emotion)) {
            // User might prefer more empathetic responses
            if (currentProfile.getConversationCount() > 5) {
                updatedProfile.setPreferredPersonalityType("empathetic");
            }
        } else if ("happy".equals(emotion) || "grateful".equals(emotion)) {
            // User might prefer casual responses
            if (currentProfile.getConversationCount() > 3) {
                updatedProfile.setPreferredPersonalityType("casual");
            }
        }
        
        logger.info("Performed basic personality update for user: {}", currentProfile.getUserId());
        return updatedProfile;
    }
    
    /**
     * Build the final chat response
     */
    private AIChatResponse buildChatResponse(Message aiMessage, UserPersonalityProfile personalityProfile, 
                                           String adaptedPersonality, Map<String, Object> nlpAnalysis, 
                                           AISafetyFilterResult safetyResult) {
        
        AIChatResponse response = new AIChatResponse();
        response.setMessageId(aiMessage.getId().toString());
        response.setResponse(aiMessage.getText());
        response.setPersonalityUsed(adaptedPersonality);
        response.setEmotionDetected((String) nlpAnalysis.get("emotion"));
        response.setSuggestedTopics(extractSuggestedTopics(aiMessage.getText()));
        response.setMetadata(Map.of(
            "confidence", nlpAnalysis.getOrDefault("confidence", 0.8),
            "sentiment", nlpAnalysis.getOrDefault("sentiment", "neutral")
        ));
        response.setSafetyFiltered(safetyResult.isSafe());
        response.setSafetyReason(safetyResult.getReason());
        response.setTimestamp(aiMessage.getTimestamp());
        response.setTokensUsed(0); // Would be set from API response
        response.setConfidence((Double) nlpAnalysis.getOrDefault("confidence", 0.8));
        response.setFollowUpQuestions(generateFollowUpQuestions(aiMessage.getText()));
        response.setConversationContext("ongoing");
        response.setNeedsHumanReview(safetyResult.isNeedsHumanReview());
        response.setReviewReason(safetyResult.getReviewReason());
        
        return response;
    }
    
    /**
     * Extract suggested topics from AI response using DeepSeek API
     */
    private List<String> extractSuggestedTopics(String response) {
        try {
            // Create topic extraction prompt
            String prompt = buildTopicExtractionPrompt(response);
            
            // Create API request
            DeepSeekModerationRequest.Message systemMsg = new DeepSeekModerationRequest.Message("system", 
                "You are an AI assistant for YouthMind, a mental health platform for youth. " +
                "Extract 3-5 relevant mental health and life topics from the given text. " +
                "Focus on topics that would be helpful for categorizing and organizing mental health conversations.");
            
            DeepSeekModerationRequest.Message userMsg = new DeepSeekModerationRequest.Message("user", prompt);
            
            DeepSeekModerationRequest apiRequest = new DeepSeekModerationRequest(
                "deepseek-chat",
                Arrays.asList(systemMsg, userMsg),
                0.3, // Lower temperature for consistent topic extraction
                200, // Fewer tokens for concise topic list
                false
            );
            
            // Call DeepSeek API
            DeepSeekModerationResponse apiResponse = callDeepSeekAPI(apiRequest);
            
            if (apiResponse.getChoices() != null && !apiResponse.getChoices().isEmpty()) {
                String aiResponse = apiResponse.getChoices().get(0).getMessage().getContent();
                return parseSuggestedTopics(aiResponse);
            }
            
            // Fallback to basic topic extraction if API fails
            return extractBasicSuggestedTopics(response);
            
        } catch (Exception e) {
            logger.error("Error extracting topics with AI: {}", e.getMessage(), e);
            return extractBasicSuggestedTopics(response);
        }
    }
    
    /**
     * Build prompt for topic extraction
     */
    private String buildTopicExtractionPrompt(String response) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Extract 3-5 relevant mental health and life topics from this text:\n\n");
        prompt.append("Text: ").append(response).append("\n\n");
        
        prompt.append("Focus on topics like:\n");
        prompt.append("- Mental health conditions (anxiety, depression, stress, etc.)\n");
        prompt.append("- Life areas (school, work, relationships, family, etc.)\n");
        prompt.append("- Emotional states (loneliness, anger, confusion, etc.)\n");
        prompt.append("- Support needs (counseling, coping strategies, etc.)\n\n");
        
        prompt.append("Format your response as a simple comma-separated list:\n");
        prompt.append("topic1, topic2, topic3, topic4, topic5\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse suggested topics from AI response
     */
    private List<String> parseSuggestedTopics(String aiResponse) {
        List<String> topics = new ArrayList<>();
        
        try {
            // Split by comma and clean up
            String[] topicArray = aiResponse.split(",");
            
            for (String topic : topicArray) {
                topic = topic.trim().toLowerCase();
                
                // Skip empty topics
                if (topic.isEmpty()) continue;
                
                // Remove common prefixes/suffixes
                topic = topic.replaceAll("^(topics?:|the|a|an)\\s+", "");
                topic = topic.replaceAll("\\s+(topics?|areas?|subjects?)$", "");
                
                // Only add if it's a reasonable length and looks like a topic
                if (topic.length() > 2 && topic.length() < 30 && !topic.matches(".*\\d+.*")) {
                    topics.add(topic);
                }
            }
            
            // Limit to 5 topics maximum
            if (topics.size() > 5) {
                topics = topics.subList(0, 5);
            }
            
            // If we didn't get any good topics, fall back to basic extraction
            if (topics.isEmpty()) {
                return extractBasicSuggestedTopics(aiResponse);
            }
            
            logger.debug("Extracted {} topics using AI", topics.size());
            return topics;
            
        } catch (Exception e) {
            logger.warn("Error parsing topics from AI response: {}", e.getMessage());
            return extractBasicSuggestedTopics(aiResponse);
        }
    }
    
    /**
     * Extract basic suggested topics as fallback
     */
    private List<String> extractBasicSuggestedTopics(String response) {
        List<String> topics = new ArrayList<>();
        String lowerResponse = response.toLowerCase();
        
        // Mental health topics
        if (lowerResponse.contains("anxiety")) topics.add("anxiety");
        if (lowerResponse.contains("depression")) topics.add("depression");
        if (lowerResponse.contains("stress")) topics.add("stress");
        if (lowerResponse.contains("panic")) topics.add("panic");
        if (lowerResponse.contains("trauma")) topics.add("trauma");
        if (lowerResponse.contains("grief")) topics.add("grief");
        if (lowerResponse.contains("loneliness")) topics.add("loneliness");
        if (lowerResponse.contains("anger")) topics.add("anger");
        if (lowerResponse.contains("confusion")) topics.add("confusion");
        
        // Life areas
        if (lowerResponse.contains("relationship")) topics.add("relationships");
        if (lowerResponse.contains("school")) topics.add("school");
        if (lowerResponse.contains("work")) topics.add("work");
        if (lowerResponse.contains("family")) topics.add("family");
        if (lowerResponse.contains("friends")) topics.add("friendship");
        if (lowerResponse.contains("health")) topics.add("health");
        if (lowerResponse.contains("future")) topics.add("future planning");
        
        // Support topics
        if (lowerResponse.contains("counseling") || lowerResponse.contains("therapy")) topics.add("counseling");
        if (lowerResponse.contains("coping")) topics.add("coping strategies");
        if (lowerResponse.contains("self-care")) topics.add("self-care");
        if (lowerResponse.contains("meditation") || lowerResponse.contains("mindfulness")) topics.add("mindfulness");
        
        // Ensure we have at least 2 topics
        if (topics.size() < 2) {
            topics.add("general support");
            topics.add("emotional well-being");
        }
        
        // Limit to 5 topics
        if (topics.size() > 5) {
            topics = topics.subList(0, 5);
        }
        
        logger.debug("Extracted {} basic topics", topics.size());
        return topics;
    }
    
    /**
     * Generate follow-up questions using DeepSeek API
     */
    private List<String> generateFollowUpQuestions(String response) {
        try {
            // Create follow-up questions prompt
            String prompt = buildFollowUpQuestionsPrompt(response);
            
            // Create API request
            DeepSeekModerationRequest.Message systemMsg = new DeepSeekModerationRequest.Message("system", 
                "You are an AI assistant for YouthMind, a mental health platform for youth. " +
                "Generate 2-3 thoughtful, supportive follow-up questions that would help continue " +
                "a meaningful conversation about mental health and emotional well-being. " +
                "Questions should be empathetic, non-intrusive, and encourage deeper sharing.");
            
            DeepSeekModerationRequest.Message userMsg = new DeepSeekModerationRequest.Message("user", prompt);
            
            DeepSeekModerationRequest apiRequest = new DeepSeekModerationRequest(
                "deepseek-chat",
                Arrays.asList(systemMsg, userMsg),
                0.7, // Higher temperature for creative questions
                300, // Moderate tokens for concise questions
                false
            );
            
            // Call DeepSeek API
            DeepSeekModerationResponse apiResponse = callDeepSeekAPI(apiRequest);
            
            if (apiResponse.getChoices() != null && !apiResponse.getChoices().isEmpty()) {
                String aiResponse = apiResponse.getChoices().get(0).getMessage().getContent();
                return parseFollowUpQuestions(aiResponse);
            }
            
            // Fallback to basic questions if API fails
            return generateBasicFollowUpQuestions(response);
            
        } catch (Exception e) {
            logger.error("Error generating follow-up questions with AI: {}", e.getMessage(), e);
            return generateBasicFollowUpQuestions(response);
        }
    }
    
    /**
     * Build prompt for follow-up questions generation
     */
    private String buildFollowUpQuestionsPrompt(String response) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Based on this AI response, generate 2-3 follow-up questions that would help continue the conversation:\n\n");
        prompt.append("AI Response: ").append(response).append("\n\n");
        
        prompt.append("Guidelines for follow-up questions:\n");
        prompt.append("- Be empathetic and supportive\n");
        prompt.append("- Encourage deeper sharing without being pushy\n");
        prompt.append("- Focus on mental health and emotional well-being\n");
        prompt.append("- Ask open-ended questions that invite reflection\n");
        prompt.append("- Avoid yes/no questions\n");
        prompt.append("- Be age-appropriate for youth (13-25)\n");
        prompt.append("- Show genuine care and interest\n\n");
        
        prompt.append("Format your response as a simple list, one question per line:\n");
        prompt.append("1. [First question]\n");
        prompt.append("2. [Second question]\n");
        prompt.append("3. [Third question]\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse follow-up questions from AI response
     */
    private List<String> parseFollowUpQuestions(String aiResponse) {
        List<String> questions = new ArrayList<>();
        
        try {
            // Split by lines and extract questions
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) continue;
                
                // Remove numbering (1., 2., 3., etc.)
                if (line.matches("^\\d+\\.\\s*.*")) {
                    line = line.replaceFirst("^\\d+\\.\\s*", "");
                }
                
                // Remove bullet points (-, *, etc.)
                if (line.matches("^[-*]\\s*.*")) {
                    line = line.replaceFirst("^[-*]\\s*", "");
                }
                
                // Clean up the question
                line = line.trim();
                
                // Only add if it looks like a question and has reasonable length
                if (line.endsWith("?") && line.length() > 10 && line.length() < 200) {
                    questions.add(line);
                }
            }
            
            // Limit to 3 questions maximum
            if (questions.size() > 3) {
                questions = questions.subList(0, 3);
            }
            
            // If we didn't get any good questions, fall back to basic ones
            if (questions.isEmpty()) {
                return generateBasicFollowUpQuestions(aiResponse);
            }
            
            logger.debug("Generated {} follow-up questions using AI", questions.size());
            return questions;
            
        } catch (Exception e) {
            logger.warn("Error parsing follow-up questions from AI response: {}", e.getMessage());
            return generateBasicFollowUpQuestions(aiResponse);
        }
    }
    
    /**
     * Generate basic follow-up questions as fallback
     */
    private List<String> generateBasicFollowUpQuestions(String response) {
        List<String> questions = new ArrayList<>();
        
        // Analyze response content for context-appropriate questions
        String lowerResponse = response.toLowerCase();
        
        if (lowerResponse.contains("anxiety") || lowerResponse.contains("worried") || lowerResponse.contains("nervous")) {
            questions.add("What specific situations tend to trigger these feelings for you?");
            questions.add("Have you found any coping strategies that help when you feel this way?");
        } else if (lowerResponse.contains("sad") || lowerResponse.contains("depressed") || lowerResponse.contains("down")) {
            questions.add("How long have you been feeling this way?");
            questions.add("What usually helps lift your mood, even just a little?");
        } else if (lowerResponse.contains("stress") || lowerResponse.contains("overwhelmed") || lowerResponse.contains("pressure")) {
            questions.add("What aspects of this situation feel most overwhelming to you?");
            questions.add("How do you typically handle stress in other areas of your life?");
        } else if (lowerResponse.contains("relationship") || lowerResponse.contains("friend") || lowerResponse.contains("family")) {
            questions.add("How has this situation affected your relationships with others?");
            questions.add("Do you feel comfortable talking to anyone in your life about this?");
        } else if (lowerResponse.contains("school") || lowerResponse.contains("work") || lowerResponse.contains("study")) {
            questions.add("How is this affecting your daily responsibilities?");
            questions.add("What would make this situation more manageable for you?");
        } else {
            // Generic supportive questions
            questions.add("How are you feeling about this situation right now?");
            questions.add("What would be most helpful for you to talk about?");
        }
        
        // Ensure we have at least 2 questions
        if (questions.size() < 2) {
            questions.add("What's on your mind about this?");
            questions.add("How can I best support you right now?");
        }
        
        // Limit to 3 questions
        if (questions.size() > 3) {
            questions = questions.subList(0, 3);
        }
        
        logger.debug("Generated {} basic follow-up questions", questions.size());
        return questions;
    }
    
    /**
     * Create safety filtered response
     */
    private AIChatResponse createSafetyFilteredResponse(AISafetyFilterResult safetyResult, AIChatRequest request) {
        AIChatResponse response = new AIChatResponse();
        response.setMessageId(UUID.randomUUID().toString());
        response.setResponse(safetyResult.getSuggestedAlternative() != null ? 
            safetyResult.getSuggestedAlternative() : 
            "I understand you want to talk about this. Let's find a way to discuss it that's helpful and safe.");
        response.setPersonalityUsed("supportive");
        response.setEmotionDetected("concerned");
        response.setSuggestedTopics(Arrays.asList("safe communication", "support resources"));
        response.setMetadata(Map.of("safety_filtered", true));
        response.setSafetyFiltered(true);
        response.setSafetyReason(safetyResult.getReason());
        response.setTimestamp(LocalDateTime.now());
        response.setTokensUsed(0);
        response.setConfidence(0.9);
        response.setFollowUpQuestions(Arrays.asList("How can I help you express this in a different way?"));
        response.setConversationContext("safety_filtered");
        response.setNeedsHumanReview(safetyResult.isNeedsHumanReview());
        response.setReviewReason(safetyResult.getReviewReason());
        
        return response;
    }
    
    /**
     * Generate safe alternative response
     */
    private String generateSafeAlternativeResponse(AIChatRequest request, UserPersonalityProfile personalityProfile, 
                                                 String adaptedPersonality) {
        return "I'm here to support you. Let's talk about what's on your mind in a way that's helpful and safe. " +
               "What would you like to discuss?";
    }
    
    /**
     * Create error response
     */
    private AIChatResponse createErrorResponse(String message, AIChatRequest request) {
        AIChatResponse response = new AIChatResponse();
        response.setMessageId(UUID.randomUUID().toString());
        response.setResponse(message);
        response.setPersonalityUsed("supportive");
        response.setEmotionDetected("neutral");
        response.setSuggestedTopics(Arrays.asList("general support"));
        response.setMetadata(Map.of("error", true));
        response.setSafetyFiltered(false);
        response.setSafetyReason("none");
        response.setTimestamp(LocalDateTime.now());
        response.setTokensUsed(0);
        response.setConfidence(0.5);
        response.setFollowUpQuestions(Arrays.asList("How can I help you today?"));
        response.setConversationContext("error");
        response.setNeedsHumanReview(false);
        response.setReviewReason("none");
        
        return response;
    }
    
    /**
     * Update user personality profile
     */
    public UserPersonalityProfile updateUserPersonalityProfile(UUID userId, UserPersonalityProfile profile) {
        return userPersonalityService.updateUserPersonalityProfile(userId, profile);
    }
    
    /**
     * Test safety filter with a sample message
     */
    public String testSafetyFilter(String testMessage, UUID userId) {
        try {
            UserPersonalityProfile profile = getUserPersonalityProfile(userId);
            Map<String, Object> nlpAnalysis = nlpAnalyzer.analyzeMessage(testMessage);
            
            AISafetyFilterResult result = safetyFilter.filterMessage(testMessage, profile, nlpAnalysis);
            
            return String.format("Safety Filter Test Results:\n" +
                "Message: %s\n" +
                "Safe: %s\n" +
                "Reason: %s\n" +
                "Severity Level: %d\n" +
                "Action Taken: %s\n" +
                "Needs Human Review: %s",
                testMessage,
                result.isSafe(),
                result.getReason(),
                result.getSeverityLevel(),
                result.getActionTaken(),
                result.isNeedsHumanReview()
            );
            
        } catch (Exception e) {
            logger.error("Error testing safety filter: {}", e.getMessage(), e);
            return "Error testing safety filter: " + e.getMessage();
        }
    }
    
    /**
     * Get performance statistics for AI features
     */
    public Map<String, Object> getPerformanceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get batching statistics
            Map<String, Object> batchingStats = requestBatchingService.getBatchStatistics();
            stats.put("batching", batchingStats);
            
            // Get database optimization statistics
            Map<String, Object> dbStats = databaseOptimizationService.getCacheStatistics();
            stats.put("databaseOptimization", dbStats);
            
            // Get cache hit rates (simplified)
            stats.put("cacheHitRate", calculateCacheHitRate());
            
            stats.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting performance statistics: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Calculate cache hit rate (simplified implementation)
     */
    private double calculateCacheHitRate() {
        // This would be implemented with proper metrics collection
        // For now, return a placeholder value
        return 0.85; // 85% cache hit rate
    }
    
    /**
     * Manually trigger personality analysis for a user
     */
    public String analyzeUserPersonality(UUID userId, String sampleMessage) {
        try {
            UserPersonalityProfile currentProfile = getUserPersonalityProfile(userId);
            Map<String, Object> nlpAnalysis = nlpAnalyzer.analyzeMessage(sampleMessage);
            
            // Create a mock request for analysis
            AIChatRequest mockRequest = new AIChatRequest();
            mockRequest.setMessage(sampleMessage);
            mockRequest.setConversationId(UUID.randomUUID().toString());
            mockRequest.setEnablePersonalityAdaptation(true);
            mockRequest.setEnableSafetyFiltering(true);
            
            // Get conversation context
            String conversationContext = getConversationContext(mockRequest.getConversationId(), userId);
            
            // Analyze personality
            UserPersonalityProfile updatedProfile = analyzeUserPersonalityWithAI(
                currentProfile, mockRequest, nlpAnalysis, "Mock AI response", conversationContext
            );
            
            // Update the profile
            String cacheKey = "user_personality:" + userId;
            redisTemplate.opsForValue().set(cacheKey, updatedProfile, 1, TimeUnit.HOURS);
            
            return String.format("Personality Analysis Results:\n" +
                "User ID: %s\n" +
                "Sample Message: %s\n" +
                "Previous Personality: %s\n" +
                "Updated Personality: %s\n" +
                "Communication Style: %s\n" +
                "Emotional State: %s\n" +
                "Interests: %s\n" +
                "Enable Humor: %s\n" +
                "Enable Empathy: %s\n" +
                "Conversation Count: %d\n" +
                "Last Updated: %s",
                userId,
                sampleMessage,
                currentProfile.getPreferredPersonalityType(),
                updatedProfile.getPreferredPersonalityType(),
                updatedProfile.getCommunicationStyle(),
                updatedProfile.getEmotionalState(),
                String.join(", ", updatedProfile.getInterests()),
                updatedProfile.isEnableHumor(),
                updatedProfile.isEnableEmpathy(),
                updatedProfile.getConversationCount(),
                updatedProfile.getLastUpdated()
            );
            
        } catch (Exception e) {
            logger.error("Error analyzing user personality: {}", e.getMessage(), e);
            return "Error analyzing user personality: " + e.getMessage();
        }
    }
    
    /**
     * Generate AI suggestions based on user message
     */
    @Transactional
    public AIChatSuggestionsResponse generateSuggestions(AIChatSuggestionsRequest request, UUID userId) {
        try {
            logger.info("Generating AI suggestions for user: {}", userId);
            
            // Get user personality profile
            UserPersonalityProfile personalityProfile = getUserPersonalityProfile(userId);
            
            // Analyze the input message
            Map<String, Object> nlpAnalysis = nlpAnalyzer.analyzeMessage(request.getMessage());
            
            // Apply safety filtering
            AISafetyFilterResult safetyResult = safetyFilter.filterMessage(
                request.getMessage(), 
                personalityProfile, 
                nlpAnalysis
            );
            
            if (!safetyResult.isSafe()) {
                return createSafetyFilteredSuggestionsResponse(safetyResult, request);
            }
            
            // Get conversation context
            String conversationContext = getConversationContext(request.getConversationId(), userId);
            
            // Adapt personality based on user profile and context
            String adaptedPersonality = personalityAdapter.adaptPersonality(
                personalityProfile, 
                nlpAnalysis, 
                conversationContext
            );
            
            // Generate AI suggestions
            List<String> suggestions = generateAISuggestions(
                request, 
                personalityProfile, 
                adaptedPersonality, 
                conversationContext,
                nlpAnalysis
            );
            
            // Create response
            AIChatSuggestionsResponse response = new AIChatSuggestionsResponse();
            response.setMessageId(UUID.randomUUID().toString());
            response.setSuggestions(suggestions);
            response.setSuggestionType(request.getSuggestionType() != null ? request.getSuggestionType() : "questions");
            response.setPersonalityUsed(adaptedPersonality);
            response.setEmotionDetected((String) nlpAnalysis.get("emotion"));
            response.setSafetyFiltered(false);
            response.setTimestamp(LocalDateTime.now());
            response.setTokensUsed(0); // Would be set by AI service
            response.setConfidence(0.8); // Would be calculated by AI service
            response.setConversationContext(conversationContext);
            response.setNeedsHumanReview(false);
            
            // Note: Message saving would be handled by the conversation service
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error generating AI suggestions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI suggestions: " + e.getMessage());
        }
    }
    
    /**
     * Analyze user message comprehensively
     */
    @Transactional
    public AIChatAnalyzeResponse analyzeMessage(AIChatAnalyzeRequest request, UUID userId) {
        try {
            logger.info("Analyzing message for user: {}", userId);
            
            // Get user personality profile
            UserPersonalityProfile personalityProfile = getUserPersonalityProfile(userId);
            
            // Analyze the input message
            Map<String, Object> nlpAnalysis = nlpAnalyzer.analyzeMessage(request.getMessage());
            
            // Apply safety filtering
            AISafetyFilterResult safetyResult = safetyFilter.filterMessage(
                request.getMessage(), 
                personalityProfile, 
                nlpAnalysis
            );
            
            if (!safetyResult.isSafe()) {
                return createSafetyFilteredAnalyzeResponse(safetyResult, request);
            }
            
            // Get conversation context
            String conversationContext = getConversationContext(request.getConversationId(), userId);
            
            // Adapt personality based on user profile and context
            String adaptedPersonality = personalityAdapter.adaptPersonality(
                personalityProfile, 
                nlpAnalysis, 
                conversationContext
            );
            
            // Perform comprehensive analysis
            Map<String, Object> analysisResult = performComprehensiveAnalysis(
                request, 
                personalityProfile, 
                adaptedPersonality, 
                conversationContext,
                nlpAnalysis
            );
            
            // Create response
            AIChatAnalyzeResponse response = new AIChatAnalyzeResponse();
            response.setMessageId(UUID.randomUUID().toString());
            response.setAnalysisResult((String) analysisResult.get("analysis"));
            response.setAnalysisType(request.getAnalysisType() != null ? request.getAnalysisType() : "comprehensive");
            response.setPersonalityUsed(adaptedPersonality);
            response.setEmotionDetected((String) nlpAnalysis.get("emotion"));
            response.setSentimentScore((String) analysisResult.get("sentiment"));
            response.setIntentDetected((String) analysisResult.get("intent"));
            response.setKeyTopics((List<String>) analysisResult.get("topics"));
            response.setRecommendations((List<String>) analysisResult.get("recommendations"));
            response.setRiskAssessment((Map<String, Object>) analysisResult.get("risk"));
            response.setSafetyFiltered(false);
            response.setTimestamp(LocalDateTime.now());
            response.setTokensUsed(0); // Would be set by AI service
            response.setConfidence(0.8); // Would be calculated by AI service
            response.setConversationContext(conversationContext);
            response.setNeedsHumanReview(false);
            response.setEmotionalInsights((Map<String, Object>) analysisResult.get("emotionalInsights"));
            response.setSuggestedActions((List<String>) analysisResult.get("actions"));
            
            // Note: Message saving would be handled by the conversation service
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error analyzing message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze message: " + e.getMessage());
        }
    }
    
    /**
     * Generate AI suggestions using AI service
     */
    private List<String> generateAISuggestions(AIChatSuggestionsRequest request, UserPersonalityProfile personalityProfile, 
                                             String adaptedPersonality, String conversationContext, Map<String, Object> nlpAnalysis) {
        try {
            // Create AI prompt for suggestions
            String prompt = buildSuggestionsPrompt(request, personalityProfile, adaptedPersonality, conversationContext, nlpAnalysis);
            
            // Call AI service for suggestions
            String aiResponse = aiServiceClient.generateCompletion(prompt, "suggestions");
            
            // Parse AI response to extract suggestions
            return parseSuggestionsFromAIResponse(aiResponse, request.getMaxSuggestions());
            
        } catch (Exception e) {
            logger.error("Error generating AI suggestions: {}", e.getMessage(), e);
            // Return fallback suggestions
            return generateFallbackSuggestions(request, personalityProfile);
        }
    }
    
    /**
     * Perform comprehensive analysis using AI service
     */
    private Map<String, Object> performComprehensiveAnalysis(AIChatAnalyzeRequest request, UserPersonalityProfile personalityProfile, 
                                                            String adaptedPersonality, String conversationContext, Map<String, Object> nlpAnalysis) {
        try {
            // Create AI prompt for analysis
            String prompt = buildAnalysisPrompt(request, personalityProfile, adaptedPersonality, conversationContext, nlpAnalysis);
            
            // Call AI service for analysis
            String aiResponse = aiServiceClient.generateCompletion(prompt, "analysis");
            
            // Parse AI response to extract analysis components
            return parseAnalysisFromAIResponse(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error performing comprehensive analysis: {}", e.getMessage(), e);
            // Return fallback analysis
            return generateFallbackAnalysis(request, personalityProfile, nlpAnalysis);
        }
    }
    
    /**
     * Build AI prompt for suggestions generation
     */
    private String buildSuggestionsPrompt(AIChatSuggestionsRequest request, UserPersonalityProfile personalityProfile, 
                                        String adaptedPersonality, String conversationContext, Map<String, Object> nlpAnalysis) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant providing suggestions for a mental health support platform. ");
        prompt.append("Generate ").append(request.getMaxSuggestions()).append(" ").append(request.getSuggestionType()).append(" based on the user's message.\n\n");
        
        prompt.append("User Message: ").append(request.getMessage()).append("\n");
        prompt.append("Personality Type: ").append(adaptedPersonality).append("\n");
        prompt.append("Emotion Detected: ").append(nlpAnalysis.get("emotion")).append("\n");
        
        if (conversationContext != null && !conversationContext.isEmpty()) {
            prompt.append("Conversation Context: ").append(conversationContext).append("\n");
        }
        
        prompt.append("\nPlease provide ").append(request.getMaxSuggestions()).append(" relevant ").append(request.getSuggestionType()).append(" that would be helpful for this user.");
        
        return prompt.toString();
    }
    
    /**
     * Build AI prompt for comprehensive analysis
     */
    private String buildAnalysisPrompt(AIChatAnalyzeRequest request, UserPersonalityProfile personalityProfile, 
                                     String adaptedPersonality, String conversationContext, Map<String, Object> nlpAnalysis) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant analyzing messages for a mental health support platform. ");
        prompt.append("Perform a comprehensive analysis of the user's message.\n\n");
        
        prompt.append("User Message: ").append(request.getMessage()).append("\n");
        prompt.append("Analysis Type: ").append(request.getAnalysisType()).append("\n");
        prompt.append("Personality Type: ").append(adaptedPersonality).append("\n");
        prompt.append("Emotion Detected: ").append(nlpAnalysis.get("emotion")).append("\n");
        
        if (conversationContext != null && !conversationContext.isEmpty()) {
            prompt.append("Conversation Context: ").append(conversationContext).append("\n");
        }
        
        prompt.append("\nPlease provide a comprehensive analysis including:\n");
        prompt.append("- Sentiment analysis\n");
        prompt.append("- Intent detection\n");
        prompt.append("- Key topics\n");
        prompt.append("- Recommendations\n");
        prompt.append("- Risk assessment\n");
        prompt.append("- Emotional insights\n");
        prompt.append("- Suggested actions\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse suggestions from AI response
     */
    private List<String> parseSuggestionsFromAIResponse(String aiResponse, int maxSuggestions) {
        List<String> suggestions = new ArrayList<>();
        
        // Simple parsing - in a real implementation, this would be more sophisticated
        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("•") || line.matches("\\d+\\.")) {
                String suggestion = line.replaceFirst("^[-•\\d+\\.\\s]+", "").trim();
                if (!suggestion.isEmpty() && suggestions.size() < maxSuggestions) {
                    suggestions.add(suggestion);
                }
            }
        }
        
        // If no suggestions found, add fallback
        if (suggestions.isEmpty()) {
            suggestions.add("How are you feeling about this situation?");
            suggestions.add("Would you like to talk more about this?");
            suggestions.add("What would be most helpful for you right now?");
        }
        
        return suggestions;
    }
    
    /**
     * Parse analysis from AI response
     */
    private Map<String, Object> parseAnalysisFromAIResponse(String aiResponse) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Simple parsing - in a real implementation, this would be more sophisticated
        analysis.put("analysis", aiResponse);
        analysis.put("sentiment", "neutral"); // Would be extracted from AI response
        analysis.put("intent", "support_seeking"); // Would be extracted from AI response
        analysis.put("topics", Arrays.asList("general", "support")); // Would be extracted from AI response
        analysis.put("recommendations", Arrays.asList("Consider talking to a professional", "Practice self-care"));
        analysis.put("risk", Map.of("level", "low", "factors", Arrays.asList()));
        analysis.put("emotionalInsights", Map.of("primary_emotion", "neutral", "intensity", "medium"));
        analysis.put("actions", Arrays.asList("Continue conversation", "Seek professional help if needed"));
        
        return analysis;
    }
    
    /**
     * Generate fallback suggestions when AI service is unavailable
     */
    private List<String> generateFallbackSuggestions(AIChatSuggestionsRequest request, UserPersonalityProfile personalityProfile) {
        List<String> suggestions = new ArrayList<>();
        
        switch (request.getSuggestionType()) {
            case "questions":
                suggestions.add("How are you feeling about this situation?");
                suggestions.add("What would be most helpful for you right now?");
                suggestions.add("Would you like to talk more about this?");
                break;
            case "responses":
                suggestions.add("I understand how you're feeling.");
                suggestions.add("That sounds really challenging.");
                suggestions.add("You're not alone in this.");
                break;
            case "topics":
                suggestions.add("Emotional support");
                suggestions.add("Coping strategies");
                suggestions.add("Professional resources");
                break;
            case "actions":
                suggestions.add("Take a deep breath");
                suggestions.add("Reach out to a friend");
                suggestions.add("Consider professional help");
                break;
            default:
                suggestions.add("How can I help you today?");
                suggestions.add("What's on your mind?");
                suggestions.add("Would you like to talk about something specific?");
        }
        
        return suggestions.subList(0, Math.min(suggestions.size(), request.getMaxSuggestions()));
    }
    
    /**
     * Generate fallback analysis when AI service is unavailable
     */
    private Map<String, Object> generateFallbackAnalysis(AIChatAnalyzeRequest request, UserPersonalityProfile personalityProfile, Map<String, Object> nlpAnalysis) {
        Map<String, Object> analysis = new HashMap<>();
        
        analysis.put("analysis", "Basic analysis completed. AI service temporarily unavailable.");
        analysis.put("sentiment", nlpAnalysis.getOrDefault("sentiment", "neutral"));
        analysis.put("intent", "support_seeking");
        analysis.put("topics", Arrays.asList("general", "support"));
        analysis.put("recommendations", Arrays.asList("Continue the conversation", "Consider professional support"));
        analysis.put("risk", Map.of("level", "low", "factors", Arrays.asList()));
        analysis.put("emotionalInsights", Map.of("primary_emotion", nlpAnalysis.getOrDefault("emotion", "neutral"), "intensity", "medium"));
        analysis.put("actions", Arrays.asList("Continue conversation", "Seek additional support if needed"));
        
        return analysis;
    }
    
    /**
     * Create safety filtered suggestions response
     */
    private AIChatSuggestionsResponse createSafetyFilteredSuggestionsResponse(AISafetyFilterResult safetyResult, AIChatSuggestionsRequest request) {
        AIChatSuggestionsResponse response = new AIChatSuggestionsResponse();
        response.setMessageId(UUID.randomUUID().toString());
        response.setSuggestions(Arrays.asList("I'm here to help in a safe and supportive way."));
        response.setSuggestionType(request.getSuggestionType() != null ? request.getSuggestionType() : "questions");
        response.setSafetyFiltered(true);
        response.setSafetyReason(safetyResult.getReason());
        response.setTimestamp(LocalDateTime.now());
        response.setNeedsHumanReview(safetyResult.isNeedsHumanReview());
        response.setReviewReason(safetyResult.getReason());
        return response;
    }
    
    /**
     * Create safety filtered analysis response
     */
    private AIChatAnalyzeResponse createSafetyFilteredAnalyzeResponse(AISafetyFilterResult safetyResult, AIChatAnalyzeRequest request) {
        AIChatAnalyzeResponse response = new AIChatAnalyzeResponse();
        response.setMessageId(UUID.randomUUID().toString());
        response.setAnalysisResult("Message blocked by safety filter: " + safetyResult.getReason());
        response.setAnalysisType(request.getAnalysisType() != null ? request.getAnalysisType() : "comprehensive");
        response.setSafetyFiltered(true);
        response.setSafetyReason(safetyResult.getReason());
        response.setTimestamp(LocalDateTime.now());
        response.setNeedsHumanReview(safetyResult.isNeedsHumanReview());
        response.setReviewReason(safetyResult.getReason());
        return response;
    }
}
