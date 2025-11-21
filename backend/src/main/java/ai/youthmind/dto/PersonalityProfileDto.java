package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for personality profile data
 * Represents comprehensive personality analysis results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityProfileDto {
    
    private UUID userId;
    private String activePersonality; // Current dominant personality traits
    private String subconsciousPersonality; // Underlying personality patterns
    private String finalPersonality; // Computed final personality assessment
    private LocalDateTime computationDate;
    private Map<String, Object> insights;
    
    // Personality dimensions
    private Double extraversion;
    private Double introversion;
    private Double openness;
    private Double conscientiousness;
    private Double agreeableness;
    private Double neuroticism;
    
    // Analysis metadata
    private String analysisMethod;
    private Double confidenceScore;
    private Integer sampleSize; // Number of interactions analyzed
    private LocalDateTime lastUpdated;
}
