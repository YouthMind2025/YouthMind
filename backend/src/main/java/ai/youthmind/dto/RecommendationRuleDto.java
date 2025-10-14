package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for recommendation mapping rules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRuleDto {
    
    /**
     * Unique rule ID
     */
    private UUID id;
    
    /**
     * Emotion combination this rule applies to (e.g., "joy,anger,sadness")
     */
    private String emotionCombination;
    
    /**
     * Mapping percentages for each emotion (should sum to 100)
     */
    private Map<String, Double> emotionPercentages;
    
    /**
     * Whether this rule is active
     */
    private Boolean isActive;
    
    /**
     * Priority of this rule (higher number = higher priority)
     */
    private Integer priority;
    
    /**
     * Description of this rule
     */
    private String description;
    
    /**
     * When this rule was created
     */
    private LocalDateTime createdAt;
    
    /**
     * When this rule was last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * User who created this rule
     */
    private UUID createdBy;
    
    /**
     * User who last updated this rule
     */
    private UUID updatedBy;
    
    /**
     * Additional metadata for this rule
     */
    private Map<String, Object> metadata;

    /**
     * Validate that percentages sum to 100
     */
    public boolean isValid() {
        if (emotionPercentages == null || emotionPercentages.isEmpty()) {
            return false;
        }
        
        double sum = emotionPercentages.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        
        return Math.abs(sum - 100.0) < 0.01; // Allow for small floating point errors
    }
}
