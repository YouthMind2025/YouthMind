package ai.youthmind.util;

import ai.youthmind.dto.AISafetyFilterResult;
import ai.youthmind.dto.UserPersonalityProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Safety filtering utility for AI chat responses
 */
@Component
public class SafetyFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SafetyFilter.class);
    
    // High-risk content patterns
    private static final List<Pattern> HIGH_RISK_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*suicide.*|.*kill.*self.*|.*end.*it.*all.*"),
        Pattern.compile("(?i).*self.*harm.*|.*cutting.*|.*hurting.*myself.*"),
        Pattern.compile("(?i).*overdose.*|.*poison.*|.*drug.*abuse.*"),
        Pattern.compile("(?i).*violence.*|.*hurt.*others.*|.*harm.*someone.*"),
        Pattern.compile("(?i).*abuse.*|.*molest.*|.*rape.*"),
        Pattern.compile("(?i).*terrorist.*|.*bomb.*|.*attack.*")
    );
    
    // Medium-risk content patterns
    private static final List<Pattern> MEDIUM_RISK_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*hate.*|.*discrimination.*|.*racist.*"),
        Pattern.compile("(?i).*bullying.*|.*harassment.*|.*intimidation.*"),
        Pattern.compile("(?i).*illegal.*|.*criminal.*|.*steal.*|.*fraud.*"),
        Pattern.compile("(?i).*addiction.*|.*substance.*abuse.*"),
        Pattern.compile("(?i).*eating.*disorder.*|.*anorexia.*|.*bulimia.*")
    );
    
    // Low-risk content patterns
    private static final List<Pattern> LOW_RISK_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*depression.*|.*anxiety.*|.*stress.*"),
        Pattern.compile("(?i).*relationship.*problems.*|.*breakup.*"),
        Pattern.compile("(?i).*family.*issues.*|.*parent.*problems.*"),
        Pattern.compile("(?i).*school.*stress.*|.*academic.*pressure.*"),
        Pattern.compile("(?i).*peer.*pressure.*|.*social.*anxiety.*")
    );
    
    // Inappropriate language patterns
    private static final List<Pattern> INAPPROPRIATE_LANGUAGE = Arrays.asList(
        Pattern.compile("(?i).*fuck.*|.*shit.*|.*damn.*|.*hell.*"),
        Pattern.compile("(?i).*bitch.*|.*asshole.*|.*idiot.*|.*stupid.*"),
        Pattern.compile("(?i).*retard.*|.*gay.*|.*fag.*|.*nigger.*")
    );
    
    // Spam patterns
    private static final List<Pattern> SPAM_PATTERNS = Arrays.asList(
        Pattern.compile("(?i).*buy.*now.*|.*click.*here.*|.*free.*money.*"),
        Pattern.compile("(?i).*win.*prize.*|.*lottery.*|.*scam.*"),
        Pattern.compile("(?i).*adult.*content.*|.*porn.*|.*xxx.*")
    );
    
    /**
     * Filter a message for safety concerns
     */
    public AISafetyFilterResult filterMessage(String message, UserPersonalityProfile userProfile, 
                                            Map<String, Object> nlpAnalysis) {
        
        try {
            logger.debug("Filtering message for safety: {}", message.substring(0, Math.min(50, message.length())));
            
            AISafetyFilterResult result = new AISafetyFilterResult();
            result.setOriginalMessage(message);
            result.setFilteredMessage(message);
            
            // Check for high-risk content
            RiskLevel highRisk = checkHighRiskContent(message);
            if (highRisk.level > 0) {
                result.setSafe(false);
                result.setReason("High-risk content detected: " + highRisk.reason);
                result.setSeverityLevel(5);
                result.setActionTaken("blocked");
                result.setNeedsHumanReview(true);
                result.setReviewReason("High-risk content requires human review");
                result.setSuggestedAlternative("I understand you're going through a difficult time. Please consider reaching out to a mental health professional or crisis hotline for immediate support.");
                return result;
            }
            
            // Check for medium-risk content
            RiskLevel mediumRisk = checkMediumRiskContent(message);
            if (mediumRisk.level > 0) {
                result.setSafe(false);
                result.setReason("Medium-risk content detected: " + mediumRisk.reason);
                result.setSeverityLevel(3);
                result.setActionTaken("filtered");
                result.setNeedsHumanReview(true);
                result.setReviewReason("Medium-risk content requires review");
                result.setSuggestedAlternative("I understand this is important to you. Let's discuss this in a way that's helpful and safe. What specific aspect would you like to explore?");
                return result;
            }
            
            // Check for inappropriate language
            RiskLevel inappropriateLanguage = checkInappropriateLanguage(message);
            if (inappropriateLanguage.level > 0) {
                result.setSafe(false);
                result.setReason("Inappropriate language detected: " + inappropriateLanguage.reason);
                result.setSeverityLevel(2);
                result.setActionTaken("filtered");
                result.setNeedsHumanReview(false);
                result.setSuggestedAlternative("I understand you're frustrated. Let's find a better way to express what you're feeling.");
                return result;
            }
            
            // Check for spam
            RiskLevel spam = checkSpamContent(message);
            if (spam.level > 0) {
                result.setSafe(false);
                result.setReason("Spam content detected: " + spam.reason);
                result.setSeverityLevel(1);
                result.setActionTaken("blocked");
                result.setNeedsHumanReview(false);
                result.setSuggestedAlternative("This appears to be promotional content. Let's focus on your mental health and wellbeing instead.");
                return result;
            }
            
            // Check user-specific triggers
            RiskLevel userTriggers = checkUserTriggers(message, userProfile);
            if (userTriggers.level > 0) {
                result.setSafe(false);
                result.setReason("User-specific trigger detected: " + userTriggers.reason);
                result.setSeverityLevel(2);
                result.setActionTaken("filtered");
                result.setNeedsHumanReview(false);
                result.setSuggestedAlternative("I notice this topic might be sensitive for you. Let's approach this carefully and focus on what's most helpful.");
                return result;
            }
            
            // Check for crisis indicators
            boolean hasCrisisIndicators = (Boolean) nlpAnalysis.getOrDefault("hasCrisisIndicators", false);
            if (hasCrisisIndicators) {
                result.setSafe(true); // Allow crisis content but flag for special handling
                result.setReason("Crisis indicators detected - requires special handling");
                result.setSeverityLevel(4);
                result.setActionTaken("approved");
                result.setNeedsHumanReview(true);
                result.setReviewReason("Crisis content requires immediate human attention");
                result.setSafetyRecommendations(Arrays.asList(
                    "Provide crisis resources",
                    "Offer immediate support",
                    "Escalate to human counselor"
                ));
                return result;
            }
            
            // Message is safe
            result.setSafe(true);
            result.setReason("Content is safe");
            result.setSeverityLevel(0);
            result.setActionTaken("approved");
            result.setNeedsHumanReview(false);
            result.setSafetyRecommendations(Arrays.asList("Continue conversation normally"));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error filtering message: {}", e.getMessage(), e);
            
            // Default to safe but flag for review
            AISafetyFilterResult result = new AISafetyFilterResult();
            result.setOriginalMessage(message);
            result.setFilteredMessage(message);
            result.setSafe(true);
            result.setReason("Filtering error - defaulting to safe");
            result.setSeverityLevel(1);
            result.setActionTaken("approved");
            result.setNeedsHumanReview(true);
            result.setReviewReason("Filtering error requires review");
            
            return result;
        }
    }
    
    /**
     * Check for high-risk content
     */
    private RiskLevel checkHighRiskContent(String message) {
        for (Pattern pattern : HIGH_RISK_PATTERNS) {
            if (pattern.matcher(message).matches()) {
                return new RiskLevel(5, "High-risk content: " + pattern.pattern());
            }
        }
        return new RiskLevel(0, "");
    }
    
    /**
     * Check for medium-risk content
     */
    private RiskLevel checkMediumRiskContent(String message) {
        for (Pattern pattern : MEDIUM_RISK_PATTERNS) {
            if (pattern.matcher(message).matches()) {
                return new RiskLevel(3, "Medium-risk content: " + pattern.pattern());
            }
        }
        return new RiskLevel(0, "");
    }
    
    /**
     * Check for inappropriate language
     */
    private RiskLevel checkInappropriateLanguage(String message) {
        for (Pattern pattern : INAPPROPRIATE_LANGUAGE) {
            if (pattern.matcher(message).matches()) {
                return new RiskLevel(2, "Inappropriate language: " + pattern.pattern());
            }
        }
        return new RiskLevel(0, "");
    }
    
    /**
     * Check for spam content
     */
    private RiskLevel checkSpamContent(String message) {
        for (Pattern pattern : SPAM_PATTERNS) {
            if (pattern.matcher(message).matches()) {
                return new RiskLevel(1, "Spam content: " + pattern.pattern());
            }
        }
        return new RiskLevel(0, "");
    }
    
    /**
     * Check for user-specific triggers
     */
    private RiskLevel checkUserTriggers(String message, UserPersonalityProfile userProfile) {
        if (userProfile == null || userProfile.getTriggers() == null) {
            return new RiskLevel(0, "");
        }
        
        String lowerMessage = message.toLowerCase();
        for (String trigger : userProfile.getTriggers()) {
            if (lowerMessage.contains(trigger.toLowerCase())) {
                return new RiskLevel(2, "User trigger: " + trigger);
            }
        }
        
        return new RiskLevel(0, "");
    }
    
    /**
     * Generate safety recommendations based on content analysis
     */
    public List<String> generateSafetyRecommendations(String message, Map<String, Object> nlpAnalysis) {
        List<String> recommendations = new ArrayList<>();
        
        boolean hasCrisisIndicators = (Boolean) nlpAnalysis.getOrDefault("hasCrisisIndicators", false);
        boolean needsSupport = (Boolean) nlpAnalysis.getOrDefault("needsSupport", false);
        String emotion = (String) nlpAnalysis.getOrDefault("emotion", "neutral");
        int urgencyLevel = (Integer) nlpAnalysis.getOrDefault("urgencyLevel", 1);
        
        if (hasCrisisIndicators) {
            recommendations.add("Provide crisis resources immediately");
            recommendations.add("Offer emergency contact information");
            recommendations.add("Escalate to human counselor");
        }
        
        if (needsSupport) {
            recommendations.add("Offer emotional support");
            recommendations.add("Provide coping strategies");
            recommendations.add("Suggest professional resources");
        }
        
        if (emotion.equals("sad") || emotion.equals("depressed")) {
            recommendations.add("Use empathetic language");
            recommendations.add("Validate feelings");
            recommendations.add("Offer hope and encouragement");
        }
        
        if (emotion.equals("anxious") || emotion.equals("worried")) {
            recommendations.add("Provide calming techniques");
            recommendations.add("Offer reassurance");
            recommendations.add("Suggest breathing exercises");
        }
        
        if (urgencyLevel >= 3) {
            recommendations.add("Respond quickly");
            recommendations.add("Use urgent language");
            recommendations.add("Offer immediate support");
        }
        
        return recommendations;
    }
    
    /**
     * Risk level data class
     */
    private static class RiskLevel {
        final int level;
        final String reason;
        
        RiskLevel(int level, String reason) {
            this.level = level;
            this.reason = reason;
        }
    }
}
