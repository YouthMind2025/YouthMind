package ai.youthmind.entity;

import ai.youthmind.enums.Emotion;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entity representing a user's recommendation vector
 * Stores emotion-based recommendation weights for personalized content delivery
 * Uses integer scale [2, 40] matching the specification
 */
@Entity
@Table(name = "user_recommendation_vectors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRecommendationVector {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull
    private UUID userId;
    
    @Column(name = "version", nullable = false)
    @NotNull
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version;
    
    // Emotion weights (2 to 40) - integer scale matching specification
    @Column(name = "sad_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Sad weight must be at least 2")
    @Max(value = 40, message = "Sad weight must be at most 40")
    private Integer sadWeight = 10;
    
    @Column(name = "fear_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Fear weight must be at least 2")
    @Max(value = 40, message = "Fear weight must be at most 40")
    private Integer fearWeight = 10;
    
    @Column(name = "angry_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Angry weight must be at least 2")
    @Max(value = 40, message = "Angry weight must be at most 40")
    private Integer angryWeight = 10;
    
    @Column(name = "happy_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Happy weight must be at least 2")
    @Max(value = 40, message = "Happy weight must be at most 40")
    private Integer happyWeight = 10;
    
    @Column(name = "conflicted_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Conflicted weight must be at least 2")
    @Max(value = 40, message = "Conflicted weight must be at most 40")
    private Integer conflictedWeight = 10;
    
    @Column(name = "surprised_weight", nullable = false)
    @NotNull
    @Min(value = 2, message = "Surprised weight must be at least 2")
    @Max(value = 40, message = "Surprised weight must be at most 40")
    private Integer surprisedWeight = 10;
    
    @Column(name = "created_at", nullable = false)
    @NotNull
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @NotNull
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get emotion weight by emotion name
     */
    public Integer getEmotionWeight(String emotion) {
        if (emotion == null) return 0;
        
        return switch (emotion.toLowerCase()) {
            case "sad" -> sadWeight;
            case "fear" -> fearWeight;
            case "angry" -> angryWeight;
            case "happy" -> happyWeight;
            case "conflicted" -> conflictedWeight;
            case "surprised" -> surprisedWeight;
            default -> 0;
        };
    }
    
    /**
     * Set emotion weight by emotion name
     */
    public void setEmotionWeight(String emotion, Integer weight) {
        if (emotion == null || weight == null) return;
        
        // Clamp value to [2, 40] range
        int clampedWeight = Math.max(2, Math.min(40, weight));
        
        switch (emotion.toLowerCase()) {
            case "sad" -> sadWeight = clampedWeight;
            case "fear" -> fearWeight = clampedWeight;
            case "angry" -> angryWeight = clampedWeight;
            case "happy" -> happyWeight = clampedWeight;
            case "conflicted" -> conflictedWeight = clampedWeight;
            case "surprised" -> surprisedWeight = clampedWeight;
            default -> {
                // Do nothing for unknown emotions
            }
        }
    }
    
    /**
     * Set emotion weight by emotion name (convenience method for int values)
     * Alias for setEmotionWeight for backward compatibility
     */
    public void setEmotionValue(String emotion, int value) {
        setEmotionWeight(emotion, value);
    }
    
    /**
     * Convenience setter methods for individual emotions (accepting int values)
     */
    public void setSad(int value) {
        this.sadWeight = Math.max(2, Math.min(40, value));
    }
    
    public void setFear(int value) {
        this.fearWeight = Math.max(2, Math.min(40, value));
    }
    
    public void setAngry(int value) {
        this.angryWeight = Math.max(2, Math.min(40, value));
    }
    
    public void setHappy(int value) {
        this.happyWeight = Math.max(2, Math.min(40, value));
    }
    
    public void setConflicted(int value) {
        this.conflictedWeight = Math.max(2, Math.min(40, value));
    }
    
    public void setSurprised(int value) {
        this.surprisedWeight = Math.max(2, Math.min(40, value));
    }
    
    /**
     * Get all emotion weights as a map
     */
    public Map<String, Integer> getAllEmotionWeights() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("sad", sadWeight);
        weights.put("fear", fearWeight);
        weights.put("angry", angryWeight);
        weights.put("happy", happyWeight);
        weights.put("conflicted", conflictedWeight);
        weights.put("surprised", surprisedWeight);
        return weights;
    }
    
    /**
     * Check if this vector has any non-zero weights
     */
    public boolean hasNonZeroWeights() {
        return sadWeight > 0 ||
               fearWeight > 0 ||
               angryWeight > 0 ||
               happyWeight > 0 ||
               conflictedWeight > 0 ||
               surprisedWeight > 0;
    }
    
    /**
     * Get the total sum of all emotion weights
     */
    public Integer getTotalWeight() {
        return sadWeight + fearWeight + angryWeight
               + happyWeight + conflictedWeight + surprisedWeight;
    }
    
    /**
     * Get the total score across all emotions
     * Alias for getTotalWeight() for backward compatibility
     * 
     * @return Sum of all emotion weights
     */
    public Integer getTotalScore() {
        return getTotalWeight();
    }
    
    /**
     * Get the top three emotions by weight with explicit tie-breaking
     * Used for determining which emotions dominate a user's preferences
     * Returns emotions sorted by weight in descending order, with tie-breaking
     * using priority order: conflicted > surprised > angry > sad > fear > happy
     * 
     * @return List of top 3 emotion names (e.g., ["sad", "fear", "angry"])
     */
    public List<String> getTopThreeEmotions() {
        // Create list of emotion-weight pairs with priority information
        List<EmotionWeightPair> emotionPairs = Arrays.asList(
            new EmotionWeightPair("conflicted", conflictedWeight, Emotion.CONFLICTED.getPriority()),
            new EmotionWeightPair("surprised", surprisedWeight, Emotion.SURPRISED.getPriority()),
            new EmotionWeightPair("angry", angryWeight, Emotion.ANGRY.getPriority()),
            new EmotionWeightPair("sad", sadWeight, Emotion.SAD.getPriority()),
            new EmotionWeightPair("fear", fearWeight, Emotion.FEAR.getPriority()),
            new EmotionWeightPair("happy", happyWeight, Emotion.HAPPY.getPriority())
        );
        
        // Sort by weight descending, then by priority ascending (lower priority number = higher priority)
        return emotionPairs.stream()
            .sorted((e1, e2) -> {
                // First compare by weight (descending)
                int weightCompare = Integer.compare(e2.weight, e1.weight);
                if (weightCompare != 0) {
                    return weightCompare;
                }
                // If weights are equal, compare by priority (ascending - lower number = higher priority)
                return Integer.compare(e1.priority, e2.priority);
            })
            .limit(3)
            .map(pair -> pair.emotion)
            .collect(Collectors.toList());
    }
    
    /**
     * Helper class for emotion-weight pairs with priority for tie-breaking
     */
    private static class EmotionWeightPair {
        final String emotion;
        final Integer weight;
        final Integer priority;
        
        EmotionWeightPair(String emotion, Integer weight, Integer priority) {
            this.emotion = emotion;
            this.weight = weight;
            this.priority = priority;
        }
    }
    
    /**
     * Check if this R vector needs halving
     * Returns true if ANY dimension > 40 (specification requirement)
     * 
     * @return true if R vector should be halved to prevent runaway growth
     */
    public boolean needsHalving() {
        return sadWeight > 40 ||
               fearWeight > 40 ||
               angryWeight > 40 ||
               happyWeight > 40 ||
               conflictedWeight > 40 ||
               surprisedWeight > 40;
    }
    
    /**
     * Halve all emotion weights with value-2 protection
     * Used when any dimension exceeds 40 to prevent numerical explosion
     * Divides all weights by 2 while maintaining proportions
     * Dimensions at value 2 are protected and remain unchanged (specification requirement)
     * Uses standard rounding (round half up)
     */
    public void halveWeights() {
        // Halve each weight, but protect values at 2 (minimum threshold)
        // Round half up: 7/2 = 3.5 → 4, 5/2 = 2.5 → 3
        if (sadWeight > 2) {
            sadWeight = (int) Math.round(sadWeight / 2.0);
        }
        if (fearWeight > 2) {
            fearWeight = (int) Math.round(fearWeight / 2.0);
        }
        if (angryWeight > 2) {
            angryWeight = (int) Math.round(angryWeight / 2.0);
        }
        if (happyWeight > 2) {
            happyWeight = (int) Math.round(happyWeight / 2.0);
        }
        if (conflictedWeight > 2) {
            conflictedWeight = (int) Math.round(conflictedWeight / 2.0);
        }
        if (surprisedWeight > 2) {
            surprisedWeight = (int) Math.round(surprisedWeight / 2.0);
        }
    }
    
    /**
     * Initialize R vector with default values (all dimensions = 10)
     * Used for new users matching specification initialization
     */
    public void initializeDefaults() {
        this.sadWeight = 10;
        this.fearWeight = 10;
        this.angryWeight = 10;
        this.happyWeight = 10;
        this.conflictedWeight = 10;
        this.surprisedWeight = 10;
    }
}
