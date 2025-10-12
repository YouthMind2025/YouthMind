package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of AI emotion classification
 * Contains the classified emotion, confidence score, and alternative emotions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of AI emotion classification")
public class EmotionClassificationResult {
    
    @Schema(description = "Primary classified emotion", example = "happy", 
            allowableValues = {"angry", "sad", "happy", "conflicted", "surprised", "fear"})
    private String emotion;
    
    @Schema(description = "Confidence score for the classification", example = "0.85", 
            minimum = "0.0", maximum = "1.0")
    private Double confidence;
    
    @Schema(description = "Source of the classification", example = "ai_classified")
    private String source;
    
    @Schema(description = "Alternative emotions with their confidence scores")
    private Map<String, Double> alternativeEmotions;
    
    /**
     * Constructor for single emotion classification
     */
    public EmotionClassificationResult(String emotion, Double confidence, String source) {
        this.emotion = emotion;
        this.confidence = confidence;
        this.source = source;
        this.alternativeEmotions = Map.of();
    }
    
    /**
     * Check if the classification result is valid
     */
    public boolean isValid() {
        return emotion != null && 
               !emotion.trim().isEmpty() && 
               confidence != null && 
               confidence >= 0.0 && 
               confidence <= 1.0 &&
               source != null &&
               !source.trim().isEmpty();
    }
    
    /**
     * Get the emotion source type
     */
    public String getSourceType() {
        return source != null ? source : "unknown";
    }
    
    /**
     * Check if this is an AI classification
     */
    public boolean isAiClassified() {
        return "ai_classified".equals(source);
    }
    
    /**
     * Get the top alternative emotion
     */
    public String getTopAlternativeEmotion() {
        if (alternativeEmotions == null || alternativeEmotions.isEmpty()) {
            return null;
        }
        
        return alternativeEmotions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    /**
     * Get the confidence of the top alternative emotion
     */
    public Double getTopAlternativeConfidence() {
        if (alternativeEmotions == null || alternativeEmotions.isEmpty()) {
            return null;
        }
        
        return alternativeEmotions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getValue)
                .orElse(null);
    }
}
