package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for storing recommendation mapping rules
 * Maps emotion combinations to percentage distributions for content recommendations
 */
@Entity
@Table(name = "recommendation_mapping_rules", indexes = {
    @Index(name = "idx_recommendation_mapping_emotion_combination", columnList = "emotionCombination"),
    @Index(name = "idx_recommendation_mapping_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationMappingRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "emotion_combination", nullable = false, unique = true, length = 255)
    @NotNull(message = "Emotion combination is required")
    @Size(min = 1, max = 255, message = "Emotion combination must be between 1 and 255 characters")
    private String emotionCombination;
    
    @Column(name = "sad_percentage", nullable = false)
    @NotNull(message = "Sad percentage is required")
    @Min(value = 0, message = "Sad percentage must be at least 0")
    @Max(value = 100, message = "Sad percentage must be at most 100")
    private Integer sadPercentage;
    
    @Column(name = "fear_percentage", nullable = false)
    @NotNull(message = "Fear percentage is required")
    @Min(value = 0, message = "Fear percentage must be at least 0")
    @Max(value = 100, message = "Fear percentage must be at most 100")
    private Integer fearPercentage;
    
    @Column(name = "angry_percentage", nullable = false)
    @NotNull(message = "Angry percentage is required")
    @Min(value = 0, message = "Angry percentage must be at least 0")
    @Max(value = 100, message = "Angry percentage must be at most 100")
    private Integer angryPercentage;
    
    @Column(name = "happy_percentage", nullable = false)
    @NotNull(message = "Happy percentage is required")
    @Min(value = 0, message = "Happy percentage must be at least 0")
    @Max(value = 100, message = "Happy percentage must be at most 100")
    private Integer happyPercentage;
    
    @Column(name = "conflicted_percentage", nullable = false)
    @NotNull(message = "Conflicted percentage is required")
    @Min(value = 0, message = "Conflicted percentage must be at least 0")
    @Max(value = 100, message = "Conflicted percentage must be at most 100")
    private Integer conflictedPercentage;
    
    @Column(name = "surprised_percentage", nullable = false)
    @NotNull(message = "Surprised percentage is required")
    @Min(value = 0, message = "Surprised percentage must be at least 0")
    @Max(value = 100, message = "Surprised percentage must be at most 100")
    private Integer surprisedPercentage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validatePercentages();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validatePercentages();
    }
    
    /**
     * Get the total percentage across all emotions
     * Should always equal 100 for valid mapping rules
     * 
     * @return Sum of all percentage values
     */
    public Integer getTotalPercentage() {
        return sadPercentage + fearPercentage + angryPercentage + 
               happyPercentage + conflictedPercentage + surprisedPercentage;
    }
    
    /**
     * Validate that all percentages sum to exactly 100
     * 
     * @throws IllegalArgumentException if percentages don't sum to 100
     */
    private void validatePercentages() {
        Integer total = getTotalPercentage();
        if (total != 100) {
            throw new IllegalArgumentException(
                String.format("Total percentage must equal 100, but got %d. " +
                            "Percentages: sad=%d, fear=%d, angry=%d, happy=%d, conflicted=%d, surprised=%d",
                            total, sadPercentage, fearPercentage, angryPercentage, 
                            happyPercentage, conflictedPercentage, surprisedPercentage)
            );
        }
    }
    
    /**
     * Get percentage for a specific emotion
     * 
     * @param emotion The emotion name (sad, fear, angry, happy, conflicted, surprised)
     * @return The percentage for the specified emotion
     * @throws IllegalArgumentException if emotion is invalid
     */
    public Integer getPercentageForEmotion(String emotion) {
        return switch (emotion.toLowerCase()) {
            case "sad" -> this.sadPercentage;
            case "fear" -> this.fearPercentage;
            case "angry" -> this.angryPercentage;
            case "happy" -> this.happyPercentage;
            case "conflicted" -> this.conflictedPercentage;
            case "surprised" -> this.surprisedPercentage;
            default -> throw new IllegalArgumentException("Invalid emotion type: " + emotion);
        };
    }
    
    /**
     * Check if this rule matches the given emotion combination
     * 
     * @param emotions List of emotions to check
     * @return true if the emotion combination matches this rule
     */
    public boolean matchesEmotionCombination(java.util.List<String> emotions) {
        if (emotions == null || emotions.isEmpty()) {
            return false;
        }
        
        // Sort emotions and create combination string
        String sortedEmotions = emotions.stream()
            .map(String::toLowerCase)
            .sorted()
            .collect(java.util.stream.Collectors.joining(","));
        
        return this.emotionCombination.equals(sortedEmotions);
    }
    
    /**
     * Get all emotion percentages as a map
     * 
     * @return Map of emotion names to percentages
     */
    public java.util.Map<String, Integer> getEmotionPercentages() {
        java.util.Map<String, Integer> percentages = new java.util.HashMap<>();
        percentages.put("sad", sadPercentage);
        percentages.put("fear", fearPercentage);
        percentages.put("angry", angryPercentage);
        percentages.put("happy", happyPercentage);
        percentages.put("conflicted", conflictedPercentage);
        percentages.put("surprised", surprisedPercentage);
        return percentages;
    }
}




