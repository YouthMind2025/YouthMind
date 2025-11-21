package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Emotion distribution percentages for feed generation")
public class EmotionDistributionDTO {
    
    @Schema(description = "Top 3 emotions used for this distribution", example = "[\"happy\", \"sad\", \"angry\"]")
    private List<String> topEmotions;
    
    @Schema(description = "Emotion combination key (e.g., 'angry,fear,sad')", example = "angry,fear,sad")
    private String emotionCombination;
    
    @NotNull
    @Min(0) @Max(100)
    @Schema(description = "Percentage for sad emotion", example = "25")
    private Integer sadPercentage;
    
    @NotNull
    @Min(0) @Max(100)
    @Schema(description = "Percentage for fear emotion", example = "25")
    private Integer fearPercentage;
    
    @NotNull
    @Min(0) @Max(100)
    @Schema(description = "Percentage for angry emotion", example = "25")
    private Integer angryPercentage;
    
    @NotNull
    @Min(0) @Max(100)
    @Schema(description = "Percentage for happy emotion", example = "15")
    private Integer happyPercentage;
    
    @NotNull
    @Min(0) @Max(100)
    @Schema(description = "Percentage for conflicted emotion", example = "5")
    private Integer conflictedPercentage;
    
    @NotNull
    @Min(0) @Max(100)
    @Schema(description = "Percentage for surprised emotion", example = "5")
    private Integer surprisedPercentage;
    
    /**
     * Validation: Total percentage must equal 100
     */
    @AssertTrue(message = "Total percentage must equal 100")
    private boolean isTotalPercentageValid() {
        if (sadPercentage == null || fearPercentage == null || angryPercentage == null ||
            happyPercentage == null || conflictedPercentage == null || surprisedPercentage == null) {
            return false;
        }
        return (sadPercentage + fearPercentage + angryPercentage + 
                happyPercentage + conflictedPercentage + surprisedPercentage) == 100;
    }
    
    /**
     * Convert to map for easier processing
     */
    public Map<String, Integer> toMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("sad", sadPercentage);
        map.put("fear", fearPercentage);
        map.put("angry", angryPercentage);
        map.put("happy", happyPercentage);
        map.put("conflicted", conflictedPercentage);
        map.put("surprised", surprisedPercentage);
        return map;
    }
    
    /**
     * Get percentage for specific emotion
     */
    public Integer getPercentageForEmotion(String emotion) {
        if (emotion == null) return 0;
        return switch (emotion.toLowerCase()) {
            case "sad" -> sadPercentage;
            case "fear" -> fearPercentage;
            case "angry" -> angryPercentage;
            case "happy" -> happyPercentage;
            case "conflicted" -> conflictedPercentage;
            case "surprised" -> surprisedPercentage;
            default -> 0;
        };
    }
}

