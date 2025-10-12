package ai.youthmind.service;

import ai.youthmind.dto.EmotionClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for AI-powered emotion classification using DeepSeek API
 * 
 * This service provides emotion classification capabilities for posts,
 * supporting both single and batch processing with rate limiting and fallback handling.
 */
@Service
@Transactional
public class AIEmotionClassificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIEmotionClassificationService.class);
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    // Emotion classification constants
    private static final List<String> VALID_EMOTIONS = Arrays.asList(
        "angry", "sad", "happy", "conflicted", "surprised", "fear"
    );
    
    private static final String EMOTION_CLASSIFICATION_PROMPT = 
        "Analyze the following text and classify the primary emotion. " +
        "Choose ONLY ONE from: angry, sad, happy, conflicted, surprised, fear. " +
        "Also provide a confidence score between 0 and 1. " +
        "Respond in this exact format: EMOTION: [emotion] CONFIDENCE: [score] " +
        "Text: {content}";
    
    // Batch processing constants
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_DELAY_MS = 1000; // 1 second delay between batches
    
    // Regex patterns for parsing DeepSeek response
    private static final Pattern EMOTION_PATTERN = Pattern.compile(
        "EMOTION:\\s*(\\w+)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile(
        "CONFIDENCE:\\s*([0-9]*\\.?[0-9]+)", Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Classify emotion for a single post content
     * 
     * @param postContent The post content to classify
     * @return EmotionClassificationResult with classified emotion and confidence
     */
    public EmotionClassificationResult classifyEmotion(String postContent) {
        logger.debug("Classifying emotion for post content: {} characters", 
                    postContent != null ? postContent.length() : 0);
        
        try {
            // Validate input
            if (postContent == null || postContent.trim().isEmpty()) {
                logger.warn("Empty post content provided for emotion classification");
                return createFallbackResult("neutral", 0.0);
            }
            
            // Truncate content if too long (DeepSeek has token limits)
            String contentToAnalyze = truncateContent(postContent, 2000);
            
            // Create prompt
            String prompt = EMOTION_CLASSIFICATION_PROMPT.replace("{content}", contentToAnalyze);
            
            logger.debug("Calling DeepSeek API for emotion classification");
            
            // Call DeepSeek API
            String response = aiServiceClient.generateCompletion(prompt, "emotion_classification");
            
            if (response == null || response.trim().isEmpty()) {
                logger.warn("Empty response from DeepSeek API for emotion classification");
                return createFallbackResult("neutral", 0.0);
            }
            
            // Parse response
            EmotionClassificationResult result = parseDeepSeekResponse(response);
            
            if (result != null && result.isValid()) {
                logger.info("Successfully classified emotion: {} with confidence: {}", 
                           result.getEmotion(), result.getConfidence());
                return result;
            } else {
                logger.warn("Failed to parse DeepSeek response: {}", response);
                return createFallbackResult("neutral", 0.0);
            }
            
        } catch (Exception e) {
            logger.error("Error during emotion classification: {}", e.getMessage(), e);
            return createFallbackResult("neutral", 0.0);
        }
    }
    
    /**
     * Classify emotions for multiple post contents in batches
     * 
     * @param postContents List of post contents to classify
     * @return List of EmotionClassificationResult objects
     */
    public List<EmotionClassificationResult> classifyEmotionBatch(List<String> postContents) {
        logger.info("Starting batch emotion classification for {} posts", postContents.size());
        
        List<EmotionClassificationResult> results = new ArrayList<>();
        
        try {
            // Process in batches to avoid rate limits
            for (int i = 0; i < postContents.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, postContents.size());
                List<String> batch = postContents.subList(i, endIndex);
                
                logger.debug("Processing batch {}/{} with {} posts", 
                           (i / BATCH_SIZE) + 1, 
                           (postContents.size() + BATCH_SIZE - 1) / BATCH_SIZE, 
                           batch.size());
                
                // Process batch
                List<EmotionClassificationResult> batchResults = processBatch(batch);
                results.addAll(batchResults);
                
                // Add delay between batches to respect rate limits
                if (endIndex < postContents.size()) {
                    try {
                        Thread.sleep(BATCH_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Batch processing interrupted");
                        break;
                    }
                }
            }
            
            logger.info("Completed batch emotion classification: {} results", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Error during batch emotion classification: {}", e.getMessage(), e);
            
            // Return fallback results for any missing classifications
            while (results.size() < postContents.size()) {
                results.add(createFallbackResult("neutral", 0.0));
            }
            
            return results;
        }
    }
    
    /**
     * Process a batch of post contents
     */
    private List<EmotionClassificationResult> processBatch(List<String> postContents) {
        List<EmotionClassificationResult> results = new ArrayList<>();
        
        for (String content : postContents) {
            try {
                EmotionClassificationResult result = classifyEmotion(content);
                results.add(result);
            } catch (Exception e) {
                logger.warn("Failed to classify emotion for content: {}", e.getMessage());
                results.add(createFallbackResult("neutral", 0.0));
            }
        }
        
        return results;
    }
    
    /**
     * Parse DeepSeek API response to extract emotion and confidence
     * 
     * @param response The raw response from DeepSeek API
     * @return Parsed EmotionClassificationResult or null if parsing fails
     */
    private EmotionClassificationResult parseDeepSeekResponse(String response) {
        try {
            logger.debug("Parsing DeepSeek response: {}", response);
            
            // Extract emotion
            Matcher emotionMatcher = EMOTION_PATTERN.matcher(response);
            String emotion = null;
            if (emotionMatcher.find()) {
                emotion = emotionMatcher.group(1).toLowerCase().trim();
            }
            
            // Extract confidence
            Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(response);
            Double confidence = null;
            if (confidenceMatcher.find()) {
                try {
                    confidence = Double.parseDouble(confidenceMatcher.group(1));
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse confidence score: {}", confidenceMatcher.group(1));
                }
            }
            
            // Validate emotion
            if (emotion != null && VALID_EMOTIONS.contains(emotion)) {
                // Ensure confidence is in valid range
                if (confidence == null) {
                    confidence = 0.5; // Default confidence
                } else {
                    confidence = Math.max(0.0, Math.min(1.0, confidence));
                }
                
                return new EmotionClassificationResult(emotion, confidence, "ai_classified");
            } else {
                logger.warn("Invalid emotion detected: {}", emotion);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error parsing DeepSeek response: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create a fallback result when AI classification fails
     */
    private EmotionClassificationResult createFallbackResult(String emotion, Double confidence) {
        return new EmotionClassificationResult(emotion, confidence, "ai_fallback");
    }
    
    /**
     * Truncate content to fit within token limits
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        
        if (content.length() <= maxLength) {
            return content;
        }
        
        // Truncate and add ellipsis
        return content.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Get the list of valid emotions
     */
    public List<String> getValidEmotions() {
        return new ArrayList<>(VALID_EMOTIONS);
    }
    
    /**
     * Check if an emotion is valid
     */
    public boolean isValidEmotion(String emotion) {
        return emotion != null && VALID_EMOTIONS.contains(emotion.toLowerCase());
    }
    
    /**
     * Get statistics about classification results
     */
    public Map<String, Object> getClassificationStats(List<EmotionClassificationResult> results) {
        Map<String, Object> stats = new HashMap<>();
        
        if (results == null || results.isEmpty()) {
            stats.put("total", 0);
            stats.put("ai_classified", 0);
            stats.put("ai_fallback", 0);
            stats.put("average_confidence", 0.0);
            return stats;
        }
        
        long aiClassified = results.stream()
                .mapToLong(r -> r.isAiClassified() ? 1 : 0)
                .sum();
        
        long aiFallback = results.stream()
                .mapToLong(r -> "ai_fallback".equals(r.getSource()) ? 1 : 0)
                .sum();
        
        double averageConfidence = results.stream()
                .filter(r -> r.getConfidence() != null)
                .mapToDouble(EmotionClassificationResult::getConfidence)
                .average()
                .orElse(0.0);
        
        stats.put("total", results.size());
        stats.put("ai_classified", aiClassified);
        stats.put("ai_fallback", aiFallback);
        stats.put("average_confidence", averageConfidence);
        
        return stats;
    }
}
