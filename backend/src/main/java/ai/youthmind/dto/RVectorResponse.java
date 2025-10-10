package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for R vector requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RVectorResponse {
    
    /**
     * User ID this R vector belongs to
     */
    private UUID userId;
    
    /**
     * R vector values (6 dimensions)
     */
    private List<Double> rVector;
    
    /**
     * Top 3 emotions based on R vector
     */
    private List<String> topEmotions;
    
    /**
     * When this R vector was last updated
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Number of interactions used to build this R vector
     */
    private Integer interactionCount;
    
    /**
     * Version of the R vector calculation algorithm
     */
    private String version;
    
    /**
     * Confidence score for this R vector (0.0 to 1.0)
     */
    private Double confidenceScore;
}
