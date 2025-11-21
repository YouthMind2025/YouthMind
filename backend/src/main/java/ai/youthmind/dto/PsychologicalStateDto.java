package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for psychological state data
 * Represents the current psychological state of a user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychologicalStateDto {
    
    private UUID userId;
    private List<Double> stateVector; // 43-dimensional psychological vector
    private Double intensityFactor;
    private Double baseAlpha;
    private LocalDateTime updatedAt;
    private Map<String, Object> metadata;
    
    // Buddhist factors analysis
    private Map<String, Double> buddhistFactors;
    
    // State indicators
    private Boolean isInitialized;
    private String stateClassification; // "stable", "volatile", "improving", "declining"
    
    // Analysis metadata
    private LocalDateTime lastAnalysisAt;
    private String analysisVersion;
    private Double confidenceScore;
}
