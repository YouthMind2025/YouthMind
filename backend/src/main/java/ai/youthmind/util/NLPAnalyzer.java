package ai.youthmind.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Natural Language Processing utility for analyzing user messages
 */
@Component
public class NLPAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(NLPAnalyzer.class);
    
    // Emotion detection patterns
    private static final Map<String, List<String>> EMOTION_PATTERNS = Map.of(
        "happy", Arrays.asList("happy", "joy", "excited", "great", "wonderful", "amazing", "fantastic", "love", "enjoy"),
        "sad", Arrays.asList("sad", "depressed", "down", "blue", "miserable", "unhappy", "crying", "tears", "hurt"),
        "angry", Arrays.asList("angry", "mad", "furious", "rage", "hate", "annoyed", "frustrated", "irritated"),
        "anxious", Arrays.asList("anxious", "worried", "nervous", "scared", "afraid", "panic", "stress", "overwhelmed"),
        "confused", Arrays.asList("confused", "lost", "unsure", "don't know", "unclear", "mixed", "torn"),
        "grateful", Arrays.asList("thankful", "grateful", "appreciate", "blessed", "lucky", "fortunate")
    );
    
    // Sentiment keywords
    private static final Map<String, List<String>> SENTIMENT_PATTERNS = Map.of(
        "positive", Arrays.asList("good", "great", "excellent", "wonderful", "amazing", "fantastic", "love", "like", "enjoy"),
        "negative", Arrays.asList("bad", "terrible", "awful", "hate", "dislike", "horrible", "worst", "sucks"),
        "neutral", Arrays.asList("okay", "fine", "alright", "normal", "regular", "average")
    );
    
    // Crisis indicators
    private static final List<String> CRISIS_INDICATORS = Arrays.asList(
        "suicide", "kill myself", "end it all", "not worth living", "better off dead",
        "hurt myself", "self harm", "cutting", "overdose", "jump", "bridge"
    );
    
    // Support request indicators
    private static final List<String> SUPPORT_INDICATORS = Arrays.asList(
        "help", "support", "advice", "guidance", "don't know what to do", "stuck",
        "struggling", "difficult", "hard time", "need help", "can't cope"
    );
    
    /**
     * Analyze a message for various NLP features
     */
    public Map<String, Object> analyzeMessage(String message) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            String lowerMessage = message.toLowerCase();
            
            // Detect emotion
            String emotion = detectEmotion(lowerMessage);
            analysis.put("emotion", emotion);
            
            // Detect sentiment
            String sentiment = detectSentiment(lowerMessage);
            analysis.put("sentiment", sentiment);
            
            // Calculate confidence
            double confidence = calculateConfidence(lowerMessage, emotion, sentiment);
            analysis.put("confidence", confidence);
            
            // Detect crisis indicators
            boolean hasCrisisIndicators = detectCrisisIndicators(lowerMessage);
            analysis.put("hasCrisisIndicators", hasCrisisIndicators);
            
            // Detect support requests
            boolean needsSupport = detectSupportRequest(lowerMessage);
            analysis.put("needsSupport", needsSupport);
            
            // Extract topics
            List<String> topics = extractTopics(lowerMessage);
            analysis.put("topics", topics);
            
            // Calculate urgency level
            int urgencyLevel = calculateUrgencyLevel(lowerMessage, hasCrisisIndicators, needsSupport);
            analysis.put("urgencyLevel", urgencyLevel);
            
            // Detect language complexity
            String complexity = detectLanguageComplexity(message);
            analysis.put("complexity", complexity);
            
            // Detect question type
            String questionType = detectQuestionType(message);
            analysis.put("questionType", questionType);
            
            logger.debug("NLP Analysis completed for message: {}", message.substring(0, Math.min(50, message.length())));
            
        } catch (Exception e) {
            logger.error("Error analyzing message: {}", e.getMessage(), e);
            analysis.put("error", "Analysis failed");
        }
        
        return analysis;
    }
    
    /**
     * Detect emotion from message content
     */
    private String detectEmotion(String message) {
        Map<String, Integer> emotionScores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : EMOTION_PATTERNS.entrySet()) {
            String emotion = entry.getKey();
            List<String> patterns = entry.getValue();
            
            int score = 0;
            for (String pattern : patterns) {
                if (message.contains(pattern)) {
                    score++;
                }
            }
            emotionScores.put(emotion, score);
        }
        
        // Find emotion with highest score
        return emotionScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
    }
    
    /**
     * Detect sentiment from message content
     */
    private String detectSentiment(String message) {
        Map<String, Integer> sentimentScores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : SENTIMENT_PATTERNS.entrySet()) {
            String sentiment = entry.getKey();
            List<String> patterns = entry.getValue();
            
            int score = 0;
            for (String pattern : patterns) {
                if (message.contains(pattern)) {
                    score++;
                }
            }
            sentimentScores.put(sentiment, score);
        }
        
        // Find sentiment with highest score
        return sentimentScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
    }
    
    /**
     * Calculate confidence score for the analysis
     */
    private double calculateConfidence(String message, String emotion, String sentiment) {
        double confidence = 0.5; // Base confidence
        
        // Increase confidence based on pattern matches
        if (!emotion.equals("neutral")) {
            confidence += 0.2;
        }
        
        if (!sentiment.equals("neutral")) {
            confidence += 0.2;
        }
        
        // Increase confidence for longer messages
        if (message.length() > 50) {
            confidence += 0.1;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    /**
     * Detect crisis indicators in the message
     */
    private boolean detectCrisisIndicators(String message) {
        return CRISIS_INDICATORS.stream()
            .anyMatch(message::contains);
    }
    
    /**
     * Detect if the message is requesting support
     */
    private boolean detectSupportRequest(String message) {
        return SUPPORT_INDICATORS.stream()
            .anyMatch(message::contains);
    }
    
    /**
     * Extract topics from the message
     */
    private List<String> extractTopics(String message) {
        List<String> topics = new ArrayList<>();
        
        // Mental health topics
        if (message.contains("anxiety") || message.contains("anxious")) {
            topics.add("anxiety");
        }
        if (message.contains("depression") || message.contains("depressed")) {
            topics.add("depression");
        }
        if (message.contains("stress") || message.contains("stressed")) {
            topics.add("stress");
        }
        if (message.contains("relationship") || message.contains("boyfriend") || message.contains("girlfriend")) {
            topics.add("relationships");
        }
        if (message.contains("school") || message.contains("college") || message.contains("study")) {
            topics.add("education");
        }
        if (message.contains("family") || message.contains("parent") || message.contains("sibling")) {
            topics.add("family");
        }
        if (message.contains("friend") || message.contains("social")) {
            topics.add("social");
        }
        if (message.contains("work") || message.contains("job") || message.contains("career")) {
            topics.add("career");
        }
        if (message.contains("health") || message.contains("medical")) {
            topics.add("health");
        }
        
        return topics;
    }
    
    /**
     * Calculate urgency level (1-5, where 5 is most urgent)
     */
    private int calculateUrgencyLevel(String message, boolean hasCrisisIndicators, boolean needsSupport) {
        int urgency = 1;
        
        if (hasCrisisIndicators) {
            urgency = 5;
        } else if (needsSupport) {
            urgency = 3;
        } else if (message.contains("urgent") || message.contains("emergency")) {
            urgency = 4;
        } else if (message.contains("important") || message.contains("serious")) {
            urgency = 2;
        }
        
        return urgency;
    }
    
    /**
     * Detect language complexity
     */
    private String detectLanguageComplexity(String message) {
        int wordCount = message.split("\\s+").length;
        int sentenceCount = message.split("[.!?]+").length;
        
        if (wordCount < 10) {
            return "simple";
        } else if (wordCount < 30) {
            return "medium";
        } else {
            return "complex";
        }
    }
    
    /**
     * Detect question type
     */
    private String detectQuestionType(String message) {
        if (message.endsWith("?")) {
            if (message.toLowerCase().startsWith("what")) {
                return "what";
            } else if (message.toLowerCase().startsWith("how")) {
                return "how";
            } else if (message.toLowerCase().startsWith("why")) {
                return "why";
            } else if (message.toLowerCase().startsWith("when")) {
                return "when";
            } else if (message.toLowerCase().startsWith("where")) {
                return "where";
            } else if (message.toLowerCase().startsWith("who")) {
                return "who";
            } else {
                return "general";
            }
        }
        return "statement";
    }
    
    /**
     * Analyze conversation context for better understanding
     */
    public Map<String, Object> analyzeConversationContext(List<String> messages) {
        Map<String, Object> contextAnalysis = new HashMap<>();
        
        try {
            // Analyze emotion progression
            List<String> emotions = new ArrayList<>();
            for (String message : messages) {
                String emotion = detectEmotion(message.toLowerCase());
                emotions.add(emotion);
            }
            contextAnalysis.put("emotionProgression", emotions);
            
            // Analyze topic consistency
            List<String> allTopics = new ArrayList<>();
            for (String message : messages) {
                allTopics.addAll(extractTopics(message.toLowerCase()));
            }
            contextAnalysis.put("topics", allTopics);
            
            // Calculate conversation sentiment trend
            List<String> sentiments = new ArrayList<>();
            for (String message : messages) {
                String sentiment = detectSentiment(message.toLowerCase());
                sentiments.add(sentiment);
            }
            contextAnalysis.put("sentimentTrend", sentiments);
            
        } catch (Exception e) {
            logger.error("Error analyzing conversation context: {}", e.getMessage(), e);
            contextAnalysis.put("error", "Context analysis failed");
        }
        
        return contextAnalysis;
    }
}
