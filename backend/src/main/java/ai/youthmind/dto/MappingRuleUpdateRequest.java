package ai.youthmind.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating recommendation mapping rule percentages
 * Validates that all percentages sum to exactly 100
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingRuleUpdateRequest {
    
    @NotNull(message = "Sad percentage is required")
    @Min(value = 0, message = "Sad percentage must be at least 0")
    @Max(value = 100, message = "Sad percentage must be at most 100")
    private Integer sadPercentage;
    
    @NotNull(message = "Fear percentage is required")
    @Min(value = 0, message = "Fear percentage must be at least 0")
    @Max(value = 100, message = "Fear percentage must be at most 100")
    private Integer fearPercentage;
    
    @NotNull(message = "Angry percentage is required")
    @Min(value = 0, message = "Angry percentage must be at least 0")
    @Max(value = 100, message = "Angry percentage must be at most 100")
    private Integer angryPercentage;
    
    @NotNull(message = "Happy percentage is required")
    @Min(value = 0, message = "Happy percentage must be at least 0")
    @Max(value = 100, message = "Happy percentage must be at most 100")
    private Integer happyPercentage;
    
    @NotNull(message = "Conflicted percentage is required")
    @Min(value = 0, message = "Conflicted percentage must be at least 0")
    @Max(value = 100, message = "Conflicted percentage must be at most 100")
    private Integer conflictedPercentage;
    
    @NotNull(message = "Surprised percentage is required")
    @Min(value = 0, message = "Surprised percentage must be at least 0")
    @Max(value = 100, message = "Surprised percentage must be at most 100")
    private Integer surprisedPercentage;
    
    /**
     * Get the total percentage across all emotions
     * Should always equal 100 for valid requests
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
    public void validatePercentages() {
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
}
