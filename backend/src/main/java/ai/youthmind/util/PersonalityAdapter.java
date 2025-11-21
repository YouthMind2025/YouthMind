package ai.youthmind.util;

import ai.youthmind.dto.UserPersonalityProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Personality adaptation utility for customizing AI responses based on user preferences
 */
@Component
public class PersonalityAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(PersonalityAdapter.class);
    
    // Personality type mappings
    private static final Map<String, Map<String, Object>> PERSONALITY_CONFIGS = Map.of(
        "supportive", Map.of(
            "tone", "warm and caring",
            "responseLength", "medium",
            "useEmpathy", true,
            "useHumor", false,
            "formality", "casual",
            "focus", "emotional support"
        ),
        "professional", Map.of(
            "tone", "clinical and structured",
            "responseLength", "detailed",
            "useEmpathy", true,
            "useHumor", false,
            "formality", "formal",
            "focus", "therapeutic guidance"
        ),
        "casual", Map.of(
            "tone", "friendly and conversational",
            "responseLength", "short",
            "useEmpathy", true,
            "useHumor", true,
            "formality", "very casual",
            "focus", "peer support"
        ),
        "empathetic", Map.of(
            "tone", "deeply understanding",
            "responseLength", "detailed",
            "useEmpathy", true,
            "useHumor", false,
            "formality", "gentle",
            "focus", "emotional validation"
        )
    );
    
    /**
     * Adapt personality based on user profile and context
     */
    public String adaptPersonality(UserPersonalityProfile userProfile, Map<String, Object> nlpAnalysis, 
                                 String conversationContext) {
        
        try {
            String basePersonality = userProfile.getPreferredPersonalityType();
            if (basePersonality == null) {
                basePersonality = "supportive";
            }
            
            // Analyze current emotional state
            String currentEmotion = (String) nlpAnalysis.getOrDefault("emotion", "neutral");
            String sentiment = (String) nlpAnalysis.getOrDefault("sentiment", "neutral");
            boolean needsSupport = (Boolean) nlpAnalysis.getOrDefault("needsSupport", false);
            boolean hasCrisisIndicators = (Boolean) nlpAnalysis.getOrDefault("hasCrisisIndicators", false);
            int urgencyLevel = (Integer) nlpAnalysis.getOrDefault("urgencyLevel", 1);
            
            // Adapt based on emotional state
            String adaptedPersonality = adaptForEmotionalState(basePersonality, currentEmotion, sentiment);
            
            // Adapt based on urgency
            adaptedPersonality = adaptForUrgency(adaptedPersonality, urgencyLevel, hasCrisisIndicators);
            
            // Adapt based on user preferences
            adaptedPersonality = adaptForUserPreferences(adaptedPersonality, userProfile);
            
            // Adapt based on conversation context
            adaptedPersonality = adaptForConversationContext(adaptedPersonality, conversationContext);
            
            logger.debug("Adapted personality from {} to {} for user {}", 
                basePersonality, adaptedPersonality, userProfile.getUserId());
            
            return adaptedPersonality;
            
        } catch (Exception e) {
            logger.error("Error adapting personality: {}", e.getMessage(), e);
            return "supportive"; // Fallback to safe default
        }
    }
    
    /**
     * Adapt personality based on emotional state
     */
    private String adaptForEmotionalState(String basePersonality, String emotion, String sentiment) {
        switch (emotion) {
            case "sad", "depressed" -> {
                if (sentiment.equals("negative")) {
                    return "empathetic"; // More empathetic for sad/negative states
                }
            }
            case "anxious", "worried" -> {
                return "supportive"; // Supportive for anxiety
            }
            case "angry", "frustrated" -> {
                return "professional"; // Professional for anger to maintain boundaries
            }
            case "confused" -> {
                return "casual"; // Casual for confusion to reduce pressure
            }
            case "happy", "grateful" -> {
                return "casual"; // Casual for positive emotions
            }
            default -> {
                return basePersonality; // Keep base personality for neutral states
            }
        }
        return basePersonality;
    }
    
    /**
     * Adapt personality based on urgency level
     */
    private String adaptForUrgency(String personality, int urgencyLevel, boolean hasCrisisIndicators) {
        if (hasCrisisIndicators || urgencyLevel >= 4) {
            return "professional"; // Professional for crisis situations
        } else if (urgencyLevel >= 3) {
            return "supportive"; // Supportive for high urgency
        }
        return personality; // Keep current personality for low urgency
    }
    
    /**
     * Adapt personality based on user preferences
     */
    private String adaptForUserPreferences(String personality, UserPersonalityProfile userProfile) {
        // Check if user has specific preferences that override the current personality
        String communicationStyle = userProfile.getCommunicationStyle();
        
        if (communicationStyle != null) {
            switch (communicationStyle) {
                case "formal" -> {
                    if (personality.equals("casual")) {
                        return "professional"; // Upgrade casual to professional for formal users
                    }
                }
                case "informal" -> {
                    if (personality.equals("professional")) {
                        return "supportive"; // Downgrade professional to supportive for informal users
                    }
                }
                case "direct" -> {
                    return "professional"; // Direct users prefer professional approach
                }
                case "gentle" -> {
                    return "empathetic"; // Gentle users prefer empathetic approach
                }
            }
        }
        
        // Check if user prefers humor
        if (userProfile.isEnableHumor() && personality.equals("professional")) {
            return "casual"; // Switch to casual if user likes humor
        }
        
        // Check if user prefers empathy
        if (userProfile.isEnableEmpathy() && personality.equals("casual")) {
            return "supportive"; // Switch to supportive if user needs more empathy
        }
        
        return personality;
    }
    
    /**
     * Adapt personality based on conversation context
     */
    private String adaptForConversationContext(String personality, String conversationContext) {
        if (conversationContext == null || conversationContext.equals("New conversation")) {
            return personality; // Keep current personality for new conversations
        }
        
        // Analyze conversation context for patterns
        String lowerContext = conversationContext.toLowerCase();
        
        // If conversation has been very formal, maintain professional tone
        if (lowerContext.contains("thank you") && lowerContext.contains("please")) {
            return "professional";
        }
        
        // If conversation has been casual, maintain casual tone
        if (lowerContext.contains("lol") || lowerContext.contains("haha") || lowerContext.contains("omg")) {
            return "casual";
        }
        
        // If conversation has been emotional, maintain empathetic tone
        if (lowerContext.contains("feel") && lowerContext.contains("hurt")) {
            return "empathetic";
        }
        
        return personality;
    }
    
    /**
     * Get personality configuration for a given personality type
     */
    public Map<String, Object> getPersonalityConfig(String personalityType) {
        return PERSONALITY_CONFIGS.getOrDefault(personalityType, PERSONALITY_CONFIGS.get("supportive"));
    }
    
    /**
     * Generate personality-specific response modifications
     */
    public Map<String, Object> generateResponseModifications(String personalityType, UserPersonalityProfile userProfile) {
        Map<String, Object> modifications = new HashMap<>();
        Map<String, Object> config = getPersonalityConfig(personalityType);
        
        // Apply personality-specific modifications
        modifications.put("tone", config.get("tone"));
        modifications.put("responseLength", config.get("responseLength"));
        modifications.put("useEmpathy", config.get("useEmpathy"));
        modifications.put("useHumor", config.get("useHumor"));
        modifications.put("formality", config.get("formality"));
        modifications.put("focus", config.get("focus"));
        
        // Apply user-specific modifications
        if (userProfile.getPreferredResponseLength() != null && !userProfile.getPreferredResponseLength().isEmpty()) {
            modifications.put("responseLength", userProfile.getPreferredResponseLength().get(0));
        }
        
        if (!userProfile.isEnableHumor()) {
            modifications.put("useHumor", false);
        }
        
        if (!userProfile.isEnableEmpathy()) {
            modifications.put("useEmpathy", false);
        }
        
        return modifications;
    }
    
    /**
     * Analyze personality effectiveness based on user engagement
     */
    public String analyzePersonalityEffectiveness(String personalityType, UserPersonalityProfile userProfile, 
                                                Map<String, Object> interactionMetrics) {
        
        // This would analyze how well the personality is working for the user
        // based on their engagement patterns, response times, etc.
        
        try {
            int conversationCount = userProfile.getConversationCount();
            Map<String, Integer> topicEngagement = userProfile.getTopicEngagement();
            
            // Simple effectiveness analysis
            if (conversationCount > 10) {
                // User has had many conversations, check if we should adjust
                if (personalityType.equals("professional") && topicEngagement.containsKey("casual_topics")) {
                    return "casual"; // Switch to casual if user engages with casual topics
                } else if (personalityType.equals("casual") && topicEngagement.containsKey("serious_topics")) {
                    return "supportive"; // Switch to supportive if user discusses serious topics
                }
            }
            
            return personalityType; // Keep current personality if it's working
            
        } catch (Exception e) {
            logger.error("Error analyzing personality effectiveness: {}", e.getMessage(), e);
            return personalityType;
        }
    }
    
    /**
     * Get personality transition suggestions
     */
    public List<String> getPersonalityTransitionSuggestions(String currentPersonality, UserPersonalityProfile userProfile) {
        List<String> suggestions = new ArrayList<>();
        
        switch (currentPersonality) {
            case "supportive" -> {
                suggestions.add("casual"); // Can become more casual
                suggestions.add("empathetic"); // Can become more empathetic
            }
            case "professional" -> {
                suggestions.add("supportive"); // Can become more supportive
            }
            case "casual" -> {
                suggestions.add("supportive"); // Can become more supportive
                suggestions.add("empathetic"); // Can become more empathetic
            }
            case "empathetic" -> {
                suggestions.add("supportive"); // Can become more supportive
            }
        }
        
        return suggestions;
    }
}
