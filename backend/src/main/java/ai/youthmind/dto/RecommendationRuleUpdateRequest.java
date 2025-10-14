package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for updating recommendation rules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRuleUpdateRequest {
    
    /**
     * Emotion combination this rule applies to (e.g., "joy,anger,sadness")
     */
    @NotBlank(message = "Emotion combination is required")
    private String emotionCombination;
    
    /**
     * Mapping percentages for each emotion (should sum to 100)
     */
    @NotEmpty(message = "Emotion percentages are required")
    private Map<@NotBlank String, @NotNull @DecimalMin(value = "0.0", message = "Percentage must be >= 0") @DecimalMax(value = "100.0", message = "Percentage must be <= 100") Double> emotionPercentages;
    
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
