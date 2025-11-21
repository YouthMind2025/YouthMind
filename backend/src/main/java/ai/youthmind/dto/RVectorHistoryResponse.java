package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for R vector history requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RVectorHistoryResponse {
    
    /**
     * User ID
     */
    private UUID userId;
    
    /**
     * List of historical R vector states
     */
    private List<RVectorHistoryItem> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RVectorHistoryItem {
        /**
         * R vector values at this point in time
         */
        private List<Double> rVector;
        
        /**
         * Top 3 emotions at this point
         */
        private List<String> topEmotions;
        
        /**
         * Timestamp when this R vector was active
         */
        private LocalDateTime timestamp;
        
        /**
         * Interaction count at this point
         */
        private Integer interactionCount;
        
        /**
         * Event that triggered this R vector update
         */
        private String triggerEvent;
        
        /**
         * Confidence score at this point
         */
        private Double confidenceScore;
    }
}
