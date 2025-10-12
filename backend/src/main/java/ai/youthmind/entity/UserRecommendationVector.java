package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's recommendation vector
 * Stores emotion-based recommendation weights for personalized content delivery
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
    
    // Emotion weights (0.0 to 1.0)
    @Column(name = "sad_weight", nullable = false, precision = 5, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0000", message = "Sad weight must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Sad weight must be at most 1.0000")
    private BigDecimal sadWeight;
    
    @Column(name = "fear_weight", nullable = false, precision = 5, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0000", message = "Fear weight must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Fear weight must be at most 1.0000")
    private BigDecimal fearWeight;
    
    @Column(name = "angry_weight", nullable = false, precision = 5, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0000", message = "Angry weight must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Angry weight must be at most 1.0000")
    private BigDecimal angryWeight;
    
    @Column(name = "happy_weight", nullable = false, precision = 5, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0000", message = "Happy weight must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Happy weight must be at most 1.0000")
    private BigDecimal happyWeight;
    
    @Column(name = "conflicted_weight", nullable = false, precision = 5, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0000", message = "Conflicted weight must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Conflicted weight must be at most 1.0000")
    private BigDecimal conflictedWeight;
    
    @Column(name = "surprised_weight", nullable = false, precision = 5, scale = 4)
    @NotNull
    @DecimalMin(value = "0.0000", message = "Surprised weight must be at least 0.0000")
    @DecimalMax(value = "1.0000", message = "Surprised weight must be at most 1.0000")
    private BigDecimal surprisedWeight;
    
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
    public BigDecimal getEmotionWeight(String emotion) {
        if (emotion == null) return BigDecimal.ZERO;
        
        return switch (emotion.toLowerCase()) {
            case "sad" -> sadWeight;
            case "fear" -> fearWeight;
            case "angry" -> angryWeight;
            case "happy" -> happyWeight;
            case "conflicted" -> conflictedWeight;
            case "surprised" -> surprisedWeight;
            default -> BigDecimal.ZERO;
        };
    }
    
    /**
     * Set emotion weight by emotion name
     */
    public void setEmotionWeight(String emotion, BigDecimal weight) {
        if (emotion == null || weight == null) return;
        
        switch (emotion.toLowerCase()) {
            case "sad" -> sadWeight = weight;
            case "fear" -> fearWeight = weight;
            case "angry" -> angryWeight = weight;
            case "happy" -> happyWeight = weight;
            case "conflicted" -> conflictedWeight = weight;
            case "surprised" -> surprisedWeight = weight;
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
        setEmotionWeight(emotion, BigDecimal.valueOf(value));
    }
    
    /**
     * Convenience setter methods for individual emotions (accepting int values)
     */
    public void setSad(int value) {
        this.sadWeight = BigDecimal.valueOf(value);
    }
    
    public void setFear(int value) {
        this.fearWeight = BigDecimal.valueOf(value);
    }
    
    public void setAngry(int value) {
        this.angryWeight = BigDecimal.valueOf(value);
    }
    
    public void setHappy(int value) {
        this.happyWeight = BigDecimal.valueOf(value);
    }
    
    public void setConflicted(int value) {
        this.conflictedWeight = BigDecimal.valueOf(value);
    }
    
    public void setSurprised(int value) {
        this.surprisedWeight = BigDecimal.valueOf(value);
    }
    
    /**
     * Get all emotion weights as a map
     */
    public java.util.Map<String, BigDecimal> getAllEmotionWeights() {
        java.util.Map<String, BigDecimal> weights = new java.util.HashMap<>();
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
        return sadWeight.compareTo(BigDecimal.ZERO) > 0 ||
               fearWeight.compareTo(BigDecimal.ZERO) > 0 ||
               angryWeight.compareTo(BigDecimal.ZERO) > 0 ||
               happyWeight.compareTo(BigDecimal.ZERO) > 0 ||
               conflictedWeight.compareTo(BigDecimal.ZERO) > 0 ||
               surprisedWeight.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get the total sum of all emotion weights
     */
    public BigDecimal getTotalWeight() {
        return sadWeight.add(fearWeight).add(angryWeight)
               .add(happyWeight).add(conflictedWeight).add(surprisedWeight);
    }
    
    /**
     * Normalize all weights to sum to 1.0
     */
    public void normalize() {
        BigDecimal total = getTotalWeight();
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            sadWeight = sadWeight.divide(total, 4, BigDecimal.ROUND_HALF_UP);
            fearWeight = fearWeight.divide(total, 4, BigDecimal.ROUND_HALF_UP);
            angryWeight = angryWeight.divide(total, 4, BigDecimal.ROUND_HALF_UP);
            happyWeight = happyWeight.divide(total, 4, BigDecimal.ROUND_HALF_UP);
            conflictedWeight = conflictedWeight.divide(total, 4, BigDecimal.ROUND_HALF_UP);
            surprisedWeight = surprisedWeight.divide(total, 4, BigDecimal.ROUND_HALF_UP);
        }
    }
}
