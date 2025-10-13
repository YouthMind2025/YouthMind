package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for feed status requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedStatusResponse {
    
    /**
     * Current block number for the user
     */
    private Integer currentBlock;
    
    /**
     * R vector summary
     */
    private RVectorSummary rVectorSummary;
    
    /**
     * Next block availability information
     */
    private NextBlockAvailability nextBlockAvailability;
    
    /**
     * User activity status
     */
    private UserActivityStatus userActivityStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RVectorSummary {
        /**
         * Current R vector values (6 dimensions)
         */
        private List<Double> rVector;
        
        /**
         * Top 3 emotions
         */
        private List<String> topEmotions;
        
        /**
         * Last updated timestamp
         */
        private LocalDateTime lastUpdated;
        
        /**
         * Number of interactions used to build this R vector
         */
        private Integer interactionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextBlockAvailability {
        /**
         * Whether the next block is available
         */
        private Boolean isAvailable;
        
        /**
         * Block number that's available
         */
        private Integer blockNumber;
        
        /**
         * When it was generated
         */
        private LocalDateTime generatedAt;
        
        /**
         * Estimated time until next block is ready (if not available)
         */
        private Integer estimatedWaitTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityStatus {
        /**
         * Whether user is currently active
         */
        private Boolean isActive;
        
        /**
         * Last scroll timestamp
         */
        private LocalDateTime lastScrollTime;
        
        /**
         * Total blocks consumed by user
         */
        private Integer totalBlocksConsumed;
        
        /**
         * User engagement score
         */
        private Double engagementScore;
    }
}
